package com.youlai.boot.platform.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.core.web.PageResult;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDto;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDto;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDto;
import com.youlai.boot.platform.ai.model.query.AiAssistantPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiAssistantRecordVo;
import com.youlai.boot.platform.ai.service.AiAssistantRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 助手控制器
 * <p>
 * 负责 AI 命令的解析、执行、记录管理及回滚操作，
 * 表示一次 AI 助手完整的指令生命周期。
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Tag(name = "AI 助手接口")
@RestController
@RequestMapping("/api/v1/ai/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiAssistantController {

  private final AiAssistantRecordService aiAssistantRecordService;

  @Operation(summary = "解析自然语言命令")
  @PostMapping("/parse")
  public Result<AiParseResponseDto> parseCommand(
    @RequestBody AiParseRequestDto request,
    HttpServletRequest httpRequest
  ) {
    log.info("收到 AI 命令解析请求: {}", request.getCommand());
    try {
      AiParseResponseDto response = aiAssistantRecordService.parseCommand(request, httpRequest);
      return Result.success(response);
    } catch (Exception e) {
      log.error("命令解析失败", e);
      return Result.success(AiParseResponseDto.builder()
        .success(false)
        .error(e.getMessage())
        .build());
    }
  }

  @Operation(summary = "执行已解析的命令")
  @PostMapping("/execute")
  public Result<Object> executeCommand(
    @RequestBody AiExecuteRequestDto request,
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
  public PageResult<AiAssistantRecordVo> getRecordPage(AiAssistantPageQuery queryParams) {
    IPage<AiAssistantRecordVo> page = aiAssistantRecordService.getRecordPage(queryParams);
    return PageResult.success(page);
  }

  @Operation(summary = "删除 AI 命令记录")
  @DeleteMapping("/records/{ids}")
  public Result<Void> deleteRecords(
    @Parameter(description = "记录ID，多个以英文逗号(,)分割")
    @PathVariable String ids
  ) {
    List<Long> idList = Arrays.stream(ids.split(","))
      .filter(s -> s != null && !s.isBlank())
      .map(String::trim)
      .map(Long::valueOf)
      .collect(Collectors.toList());

    boolean removed = aiAssistantRecordService.deleteRecords(idList);
    return Result.judge(removed);
  }

  @Operation(summary = "撤销命令执行")
  @PostMapping("/records/{recordId}/rollback")
  public Result<Void> rollbackCommand(
    @Parameter(description = "记录ID")
    @PathVariable String recordId
  ) {
    aiAssistantRecordService.rollbackCommand(recordId);
    return Result.success();
  }
}
