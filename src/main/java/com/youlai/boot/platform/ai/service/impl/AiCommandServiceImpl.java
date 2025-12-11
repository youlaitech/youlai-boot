package com.youlai.boot.platform.ai.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiFunctionCallDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDTO;
import com.youlai.boot.platform.ai.model.entity.AiCommandRecord;
import com.youlai.boot.platform.ai.service.AiCommandRecordService;
import com.youlai.boot.platform.ai.service.AiCommandService;
import com.youlai.boot.platform.ai.tools.UserTools;
import com.youlai.boot.security.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 命令编排服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiCommandServiceImpl implements AiCommandService {

    private static final String SYSTEM_PROMPT = """
            你是一个智能的企业操作助手，需要将用户的自然语言命令解析成标准的函数调用。
            请返回严格的 JSON 格式，包含字段：
            - success: boolean
            - explanation: string
            - confidence: number (0-1)
            - error: string
            - provider: string
            - model: string
            - functionCalls: 数组，每个元素包含 name、description、arguments(对象)
            当无法识别命令时，success=false，并给出 error。
            """;

    private final AiCommandRecordService recordService;
    private final UserTools userTools;
    private final ChatClient chatClient;

    @Override
    public AiParseResponseDTO parseCommand(AiParseRequestDTO request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        String command = Optional.ofNullable(request.getCommand()).orElse("").trim();

        if (StrUtil.isBlank(command)) {
            return AiParseResponseDTO.builder()
                    .success(false)
                    .error("命令不能为空")
                    .functionCalls(Collections.emptyList())
                    .build();
        }

        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String ipAddress = JakartaServletUtil.getClientIP(httpRequest);

        AiCommandRecord commandRecord = new AiCommandRecord();
        commandRecord.setUserId(userId);
        commandRecord.setUsername(username);
        commandRecord.setOriginalCommand(command);
        commandRecord.setIpAddress(ipAddress);
        commandRecord.setAiProvider("spring-ai");
        commandRecord.setAiModel("auto");

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        try {
            log.info("📤 发送命令至 AI 模型: {}", command);
            ChatResponse chatResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call().chatResponse();

            String rawContent = Optional.ofNullable(chatResponse.getResult())
                    .map(result -> result.getOutput().getText())
                    .orElse("");

            ParseResult parseResult = parseAiResponse(rawContent);

            commandRecord.setAiProvider(StrUtil.emptyToDefault(parseResult.provider(), "spring-ai"));
            commandRecord.setAiModel(StrUtil.emptyToDefault(parseResult.model(), "auto"));
            commandRecord.setParseStatus(parseResult.success() ? 1 : 0);
            commandRecord.setExplanation(parseResult.explanation());
            commandRecord.setFunctionCalls(JSONUtil.toJsonStr(parseResult.functionCalls()));
            commandRecord.setConfidence(parseResult.confidence() != null ? BigDecimal.valueOf(parseResult.confidence()) : null);
            commandRecord.setParseErrorMessage(parseResult.success() ? null : StrUtil.emptyToDefault(parseResult.error(), "解析失败"));
            long duration = System.currentTimeMillis() - startTime;
            commandRecord.setParseDurationMs((int) duration);

            recordService.save(commandRecord);

            AiParseResponseDTO response = AiParseResponseDTO.builder()
                    .parseLogId(commandRecord.getId())
                    .success(parseResult.success())
                    .functionCalls(parseResult.functionCalls())
                    .explanation(parseResult.explanation())
                    .confidence(parseResult.confidence())
                    .error(parseResult.error())
                    .rawResponse(rawContent)
                    .build();

            if (!parseResult.success()) {
                log.warn("❗️ AI 未能解析命令: {}", parseResult.error());
            } else {
                log.info("✅ 解析成功，审计记录ID: {}", commandRecord.getId());
            }

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            commandRecord.setParseStatus(0);
            commandRecord.setFunctionCalls(JSONUtil.toJsonStr(Collections.emptyList()));
            commandRecord.setParseErrorMessage(e.getMessage());
            commandRecord.setParseDurationMs((int) duration);
            recordService.save(commandRecord);

            log.error("❌ 解析命令失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析命令失败: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    private String buildUserPrompt(AiParseRequestDTO request) {
        JSONObject payload = JSONUtil.createObj()
                .set("command", request.getCommand())
                .set("currentRoute", request.getCurrentRoute())
                .set("currentComponent", request.getCurrentComponent())
                .set("context", Optional.ofNullable(request.getContext()).orElse(Collections.emptyMap()))
                .set("availableFunctions", availableFunctions());

        return StrUtil.format("""
                请根据以下上下文识别用户意图，并输出符合系统提示要求的 JSON：
                {}
                """, JSONUtil.toJsonPrettyStr(payload));
    }

    private List<Map<String, Object>> availableFunctions() {
        return List.of(
                Map.of(
                        "name", "updateUserNickname",
                        "description", "根据用户名更新用户昵称",
                        "requiredParameters", List.of("username", "nickname")
                )
        );
    }

    private ParseResult parseAiResponse(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            throw new IllegalStateException("AI 返回内容为空");
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(rawContent);
            boolean success = jsonObject.getBool("success", false);
            String explanation = jsonObject.getStr("explanation");
            Double confidence = jsonObject.containsKey("confidence") ? jsonObject.getDouble("confidence") : null;
            String error = jsonObject.getStr("error");
            String provider = jsonObject.getStr("provider");
            String model = jsonObject.getStr("model");

            List<AiFunctionCallDTO> functionCalls = toFunctionCallList(jsonObject.getJSONArray("functionCalls"));

            return new ParseResult(success, explanation, confidence, error, provider, model, functionCalls);
        } catch (Exception ex) {
            throw new IllegalStateException("无法解析 AI 响应: " + ex.getMessage(), ex);
        }
    }

    private List<AiFunctionCallDTO> toFunctionCallList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiFunctionCallDTO> result = new ArrayList<>();
        for (Object element : array) {
            JSONObject functionJson = JSONUtil.parseObj(element);
            Map<String, Object> arguments = Optional.ofNullable(functionJson.getJSONObject("arguments"))
                    .map(obj -> obj.toBean(new TypeReference<Map<String, Object>>() {
                    }))
                    .orElse(Collections.emptyMap());

            result.add(AiFunctionCallDTO.builder()
                    .name(functionJson.getStr("name"))
                    .description(functionJson.getStr("description"))
                    .arguments(arguments)
                    .build());
        }
        return result;
    }

    private record ParseResult(
            boolean success,
            String explanation,
            Double confidence,
            String error,
            String provider,
            String model,
            List<AiFunctionCallDTO> functionCalls
    ) {
    }

    @Override
    public Object executeCommand(AiExecuteRequestDTO request, HttpServletRequest httpRequest) throws Exception {
        long startTime = System.currentTimeMillis();

        // 获取用户信息
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String ipAddress = JakartaServletUtil.getClientIP(httpRequest);

        AiFunctionCallDTO functionCall = request.getFunctionCall();

        // 根据解析日志ID获取审计记录，如果不存在则创建新记录
        AiCommandRecord commandRecord ;
        if (StrUtil.isNotBlank(request.getParseLogId())) {
            // 更新已存在的审计记录（解析阶段已创建）
            commandRecord = recordService.getById(request.getParseLogId());
            if (commandRecord == null) {
                throw new IllegalStateException("未找到对应的解析记录，ID: " + request.getParseLogId());
            }
        } else {
            // 如果没有解析日志ID，创建新记录（兼容直接执行的情况）
            commandRecord = new AiCommandRecord();
            commandRecord.setUserId(userId);
            commandRecord.setUsername(username);
            commandRecord.setOriginalCommand(request.getOriginalCommand());
            commandRecord.setIpAddress(ipAddress);
            recordService.save(commandRecord);
        }

        // 更新执行相关字段
        commandRecord.setFunctionName(functionCall.getName());
        commandRecord.setFunctionArguments(JSONUtil.toJsonStr(functionCall.getArguments()));
        commandRecord.setExecuteStatus(0); // 0-待执行

        try {
            // 🎯 执行具体的函数调用
            Object result = executeFunctionCall(functionCall);

            // 更新执行成功
            commandRecord.setExecuteStatus(1); // 1-成功
            commandRecord.setExecuteErrorMessage(null);

            // 更新审计记录
            recordService.updateById(commandRecord);

            log.info("✅ 命令执行成功，审计记录ID: {}", commandRecord.getId());

            return result;

        } catch (Exception e) {
            // 更新执行失败
            commandRecord.setExecuteStatus(-1); // -1-失败
            commandRecord.setExecuteErrorMessage(e.getMessage());

            // 更新审计记录
            recordService.updateById(commandRecord);

            log.error("❌ 命令执行失败，审计记录ID: {}", commandRecord.getId(), e);

            // 抛出异常，由 Controller 统一处理
            throw e;
        }
    }

    /**
     * 执行具体的函数调用
     */
    private Object executeFunctionCall(AiFunctionCallDTO functionCall) {
        String functionName = functionCall.getName();
        Map<String, Object> arguments = functionCall.getArguments();

        log.info("🎯 执行函数: {}, 参数: {}", functionName, arguments);

        // 根据函数名称路由到不同的处理器
        switch (functionName) {
            case "updateUserNickname":
                return executeUpdateUserNickname(arguments);
            default:
                throw new UnsupportedOperationException("不支持的函数: " + functionName);
        }
    }

    /**
     * 使用 Tool: 根据用户名更新用户昵称
     */
    private Object executeUpdateUserNickname(Map<String, Object> arguments) {
        String username = (String) arguments.get("username");
        String nickname = (String) arguments.get("nickname");

        log.info("🔧 [Tool] 更新用户昵称: username={}, nickname={}", username, nickname);
        String resultMsg = userTools.updateUserNickname(username, nickname);

        boolean success = resultMsg != null && resultMsg.contains("成功");
        if (!success) {
            throw new RuntimeException(resultMsg != null ? resultMsg : "更新用户昵称失败");
        }

        return Map.of("username", username, "nickname", nickname, "message", resultMsg);
    }
}


