package com.youlai.boot.platform.ai.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.platform.ai.mapper.AiAssistantRecordMapper;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDto;
import com.youlai.boot.platform.ai.model.dto.AiFunctionCallDto;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDto;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDto;
import com.youlai.boot.platform.ai.model.entity.AiAssistantRecord;
import com.youlai.boot.platform.ai.model.query.AiAssistantPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiAssistantRecordVo;
import com.youlai.boot.platform.ai.service.AiAssistantRecordService;
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
 * AI 助手行为记录服务实现类
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiAssistantRecordServiceImpl
  extends ServiceImpl<AiAssistantRecordMapper, AiAssistantRecord>
  implements AiAssistantRecordService {

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

  private final UserTools userTools;
  private final ChatClient chatClient;

  @Override
  public AiParseResponseDto parseCommand(AiParseRequestDto request, HttpServletRequest httpRequest) {
    long startTime = System.currentTimeMillis();
    String command = Optional.ofNullable(request.getCommand()).orElse("").trim();

    if (StrUtil.isBlank(command)) {
      return AiParseResponseDto.builder()
        .success(false)
        .error("命令不能为空")
        .functionCalls(Collections.emptyList())
        .build();
    }

    Long userId = SecurityUtils.getUserId();
    String username = SecurityUtils.getUsername();
    String ipAddress = JakartaServletUtil.getClientIP(httpRequest);

    AiAssistantRecord commandRecord = new AiAssistantRecord();
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

      this.save(commandRecord);

      return AiParseResponseDto.builder()
        .parseLogId(commandRecord.getId())
        .success(parseResult.success())
        .functionCalls(parseResult.functionCalls())
        .explanation(parseResult.explanation())
        .confidence(parseResult.confidence())
        .error(parseResult.error())
        .rawResponse(rawContent)
        .build();
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      commandRecord.setParseStatus(0);
      commandRecord.setFunctionCalls(JSONUtil.toJsonStr(Collections.emptyList()));
      commandRecord.setParseErrorMessage(e.getMessage());
      commandRecord.setParseDurationMs((int) duration);
      this.save(commandRecord);

      log.error("❌ 解析命令失败: {}", e.getMessage(), e);
      throw new RuntimeException("解析命令失败: " + e.getMessage(), e);
    }
  }

  private String buildSystemPrompt() {
    return SYSTEM_PROMPT;
  }

  private String buildUserPrompt(AiParseRequestDto request) {
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

      List<AiFunctionCallDto> functionCalls = toFunctionCallList(jsonObject.getJSONArray("functionCalls"));

      return new ParseResult(success, explanation, confidence, error, provider, model, functionCalls);
    } catch (Exception ex) {
      throw new IllegalStateException("无法解析 AI 响应: " + ex.getMessage(), ex);
    }
  }

  private List<AiFunctionCallDto> toFunctionCallList(JSONArray array) {
    if (array == null || array.isEmpty()) {
      return Collections.emptyList();
    }

    List<AiFunctionCallDto> result = new ArrayList<>();
    for (Object element : array) {
      JSONObject functionJson = JSONUtil.parseObj(element);
      Map<String, Object> arguments = Optional.ofNullable(functionJson.getJSONObject("arguments"))
        .map(obj -> obj.toBean(new TypeReference<Map<String, Object>>() {
        }))
        .orElse(Collections.emptyMap());

      result.add(AiFunctionCallDto.builder()
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
    List<AiFunctionCallDto> functionCalls
  ) {
  }

  @Override
  public Object executeCommand(AiExecuteRequestDto request, HttpServletRequest httpRequest) throws Exception {
    Long userId = SecurityUtils.getUserId();
    String username = SecurityUtils.getUsername();
    String ipAddress = JakartaServletUtil.getClientIP(httpRequest);

    AiFunctionCallDto functionCall = request.getFunctionCall();

    AiAssistantRecord commandRecord;
    if (StrUtil.isNotBlank(request.getParseLogId())) {
      commandRecord = this.getById(request.getParseLogId());
      if (commandRecord == null) {
        throw new IllegalStateException("未找到对应的解析记录，ID: " + request.getParseLogId());
      }
    } else {
      commandRecord = new AiAssistantRecord();
      commandRecord.setUserId(userId);
      commandRecord.setUsername(username);
      commandRecord.setOriginalCommand(request.getOriginalCommand());
      commandRecord.setIpAddress(ipAddress);
      this.save(commandRecord);
    }

    commandRecord.setFunctionName(functionCall.getName());
    commandRecord.setFunctionArguments(JSONUtil.toJsonStr(functionCall.getArguments()));
    commandRecord.setExecuteStatus(0);

    try {
      Object result = executeFunctionCall(functionCall);
      commandRecord.setExecuteStatus(1);
      commandRecord.setExecuteErrorMessage(null);
      this.updateById(commandRecord);
      log.info("✅ 命令执行成功，审计记录ID: {}", commandRecord.getId());
      return result;
    } catch (Exception e) {
      commandRecord.setExecuteStatus(-1);
      commandRecord.setExecuteErrorMessage(e.getMessage());
      this.updateById(commandRecord);
      log.error("❌ 命令执行失败，审计记录ID: {}", commandRecord.getId(), e);
      throw e;
    }
  }

  private Object executeFunctionCall(AiFunctionCallDto functionCall) {
    String functionName = functionCall.getName();
    Map<String, Object> arguments = functionCall.getArguments();

    log.info("🎯 执行函数: {}, 参数: {}", functionName, arguments);

    switch (functionName) {
      case "updateUserNickname":
        return executeUpdateUserNickname(arguments);
      default:
        throw new UnsupportedOperationException("不支持的函数: " + functionName);
    }
  }

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

  @Override
  public IPage<AiAssistantRecordVo> getRecordPage(AiAssistantPageQuery queryParams) {
    Page<AiAssistantRecordVo> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
    return this.baseMapper.getRecordPage(page, queryParams);
  }

  @Override
  public boolean deleteRecords(List<Long> ids) {
    return this.removeByIds(ids);
  }

  @Override
  public void rollbackCommand(String logId) {
    AiAssistantRecord commandRecord = this.getById(logId);
    if (commandRecord == null) {
      throw new RuntimeException("命令记录不存在");
    }

    if (commandRecord.getExecuteStatus() == null || commandRecord.getExecuteStatus() != 1) {
      throw new RuntimeException("只能撤销成功执行的命令");
    }

    log.info("撤销命令执行: logId={}, function={}", logId, commandRecord.getFunctionName());
    throw new UnsupportedOperationException("回滚功能尚未实现");
  }
}
