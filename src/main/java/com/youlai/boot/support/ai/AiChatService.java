package com.youlai.boot.support.ai;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
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
        } catch (RuntimeException e) {
            log.error("AI 调用失败, {} 耗时 {} ms", operation, System.currentTimeMillis() - start, e);
            throw e;
        }
    }
}
