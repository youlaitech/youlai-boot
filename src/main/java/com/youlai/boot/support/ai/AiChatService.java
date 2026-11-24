package com.youlai.boot.support.ai;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AI 对话服务
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ObjectProvider<ChatModel> chatModelProvider;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    // 对话客户端，首次调用时构建
    private ChatClient chatClient;

    /**
     * 发送提示词并返回模型输出
     *
     * @param systemPrompt 系统提示词（角色与约束）
     * @param userPrompt   用户提示词（输入数据）
     * @return 模型输出文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        return execute("chat", () -> {
            String content = client().prompt().system(systemPrompt).user(userPrompt).call().content();
            return StrUtil.blankToDefault(content, StrUtil.EMPTY).trim();
        });
    }

    /**
     * 发送提示词并映射为实体
     *
     * @param systemPrompt 系统提示词（角色与约束）
     * @param userPrompt   用户提示词（输入数据）
     * @param type         目标实体类型
     * @return 映射后的实体
     */
    public <T> T chatEntity(String systemPrompt, String userPrompt, Class<T> type) {
        return execute("chatEntity(" + type.getSimpleName() + ")", () -> client().prompt()
                .system(systemPrompt).user(userPrompt).call()
                .entity(type, spec -> spec.validateSchema()));
    }

    /**
     * 发送提示词并映射为 JSON 数组
     *
     * @param systemPrompt 系统提示词（角色与约束）
     * @param userPrompt   用户提示词（输入数据）
     * @return JSON 数组
     */
    public List<Map<String, Object>> chatJsonList(String systemPrompt, String userPrompt) {
        return execute("chatJsonList", () -> client().prompt()
                .system(systemPrompt).user(userPrompt).call()
                .entity(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
    }

    /**
     * 获取对话客户端，首次调用时构建
     *
     * @return 对话客户端
     */
    private synchronized ChatClient client() {
        if (chatClient == null) {
            // 未装配 ChatModel 时 Builder 也建不出来，先判断模型是否存在，避免抛出框架的 BeanCreationException
            if (chatModelProvider.getIfAvailable() == null) {
                throw new BusinessException(
                        "AI 功能未开启，请将 spring.ai.model.chat 置为 openai 并配置 spring.ai.openai.api-key");
            }
            log.info("AI 对话客户端初始化");
            chatClient = chatClientBuilderProvider.getObject().build();
        }
        return chatClient;
    }

    /**
     * 执行模型调用并记录开始、耗时与异常
     *
     * @param operation 调用标识
     * @param call      模型调用
     * @return 调用结果
     */
    private <T> T execute(String operation, Supplier<T> call) {
        log.info("AI 调用开始, {}", operation);
        long start = System.currentTimeMillis();
        try {
            T result = call.get();
            log.info("AI 调用完成, {} 耗时 {} ms", operation, System.currentTimeMillis() - start);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("AI 调用失败, {} 耗时 {} ms", operation, System.currentTimeMillis() - start, e);
            throw translate(e);
        }
    }

    /**
     * 把模型服务的调用异常转成可直接展示的提示
     *
     * <p>额度用尽、密钥失效、限流、模型下线等问题由服务商返回 4xx 响应体说明，
     * 原样抛出只会变成「系统执行出错」，这里按响应内容归类成用户能看懂的原因。
     *
     * @param e 模型调用异常
     * @return 带明确原因的业务异常
     */
    private BusinessException translate(RuntimeException e) {
        String detail = collectMessage(e);
        if (StrUtil.isBlank(detail)) {
            detail = e.getClass().getSimpleName();
        }
        String text = detail.toLowerCase(Locale.ROOT);

        if (text.contains("invalid_api_key") || text.contains("invalidapikey")
                || text.contains("incorrect api key") || text.contains("apikey must not be empty")
                || text.contains("401") || text.contains("unauthorized")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 密钥无效或未配置，请检查 spring.ai.openai.api-key");
        }
        if (text.contains("rate limit") || text.contains("throttling") || text.contains("429")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 请求过于频繁，请稍后重试");
        }
        if (text.contains("allocationquota") || text.contains("freetieronly") || text.contains("arrearage")
                || text.contains("insufficient_quota") || text.contains("quota") || text.contains("free tier")
                || text.contains("exhausted") || text.contains("in good standing")
                || text.contains("额度") || text.contains("欠费")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 模型额度已用尽或免费额度已过期，请在模型服务平台充值或更换模型");
        }
        if (text.contains("model.notfound") || text.contains("model_not_found")
                || text.contains("model not exist") || text.contains("model_not_supported")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 模型不存在或已下线，请检查 spring.ai.openai.chat.model");
        }
        if (text.contains("403") || text.contains("permission denied") || text.contains("access denied")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 服务拒绝访问，请确认密钥有效且账号已开通该模型");
        }
        if (text.contains("404")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 接口地址不存在，请检查 spring.ai.openai.base-url");
        }
        if (e instanceof ResourceAccessException || text.contains("timed out")
                || text.contains("connection refused") || text.contains("unknownhost")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 服务连接失败或响应超时，请检查网络与 spring.ai.openai.base-url");
        }
        if (text.contains("500") || text.contains("502") || text.contains("503")
                || text.contains("service unavailable")) {
            return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                    "AI 服务暂时不可用，请稍后重试");
        }
        return new BusinessException(ResultCode.THIRD_PARTY_SERVICE_ERROR,
                "AI 调用失败：" + StrUtil.maxLength(detail, 200));
    }

    /**
     * 拼接异常链上的信息，服务商把错误原因放在响应体里，只取最外层会丢掉关键内容
     *
     * @param e 模型调用异常
     * @return 错误信息
     */
    private String collectMessage(RuntimeException e) {
        StringBuilder message = new StringBuilder(StrUtil.blankToDefault(e.getMessage(), StrUtil.EMPTY));
        Throwable cause = e.getCause();
        for (int i = 0; cause != null && i < 5; i++) {
            if (StrUtil.isNotBlank(cause.getMessage())) {
                message.append(' ').append(cause.getMessage());
            }
            cause = cause.getCause();
        }
        return message.toString();
    }
}
