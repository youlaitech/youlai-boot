package com.youlai.boot.platform.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.core.web.PageResult;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDTO;
import com.youlai.boot.platform.ai.model.query.AiAssistantQuery;
import com.youlai.boot.platform.ai.model.vo.AiAssistantRecordVO;
import com.youlai.boot.platform.ai.service.AiAssistantRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 助手控制器
 * <p>
 * 负责 AI 命令的解析、执行与记录查询。
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Tag(name = "13.AI 助手接口")
@RestController
@RequestMapping("/api/v1/ai/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiAssistantController {

  private final AiAssistantRecordService aiAssistantRecordService;

  @Operation(summary = "解析自然语言命令")
  @PostMapping("/parse")
  public Result<AiParseResponseDTO> parseCommand(
    @RequestBody AiParseRequestDTO request,
    HttpServletRequest httpRequest
  ) {
    log.info("收到 AI 命令解析请求: {}", request.getCommand());
    try {
      AiParseResponseDTO response = aiAssistantRecordService.parseCommand(request, httpRequest);
      return Result.success(response);
    } catch (Exception e) {
      log.error("命令解析失败", e);
      return Result.success(AiParseResponseDTO.builder()
        .success(false)
        .error(e.getMessage())
        .build());
    }
  }

  @Operation(summary = "执行已解析的命令")
  @PostMapping("/execute")
  public Result<Object> executeCommand(
    @RequestBody AiExecuteRequestDTO request,
    HttpServletRequest httpRequest
  ) {
    log.info("收到 AI 命令执行请求: {}", request.getFunctionCall().getName());
    try {
      Object result = aiAssistantRecordService.executeCommand(request, httpRequest);
      return Result.success(result);
    } catch (Exception e) {
      log.error("命令执行失败", e);
      return Result.failed(e.getMessage());
    }
  }

  @Operation(summary = "获取 AI 命令记录分页列表")
  @GetMapping("/records")
  public PageResult<AiAssistantRecordVO> getRecordPage(AiAssistantQuery queryParams) {
    IPage<AiAssistantRecordVO> page = aiAssistantRecordService.getRecordPage(queryParams);
    return PageResult.success(page);
  }

  // 记录类接口按需扩展，当前开放 parse/execute/records
}
