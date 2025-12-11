package com.youlai.boot.platform.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.core.web.PageResult;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDTO;
import com.youlai.boot.platform.ai.model.query.AiCommandPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiCommandRecordVO;
import com.youlai.boot.platform.ai.service.AiCommandRecordService;
import com.youlai.boot.platform.ai.service.AiCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 命令控制器（基于 Spring AI）
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Tag(name = "AI命令接口")
@RestController
@RequestMapping("/api/v1/ai/command")
@RequiredArgsConstructor
@Slf4j
public class AiCommandController {

  private final AiCommandService aiCommandService;
  private final AiCommandRecordService recordService;

  @Operation(summary = "解析自然语言命令")
  @PostMapping("/parse")
  public Result<AiParseResponseDTO> parseCommand(
    @RequestBody AiParseRequestDTO request,
    HttpServletRequest httpRequest
  ) {
    log.info("收到AI命令解析请求: {}", request.getCommand());

    try {
      AiParseResponseDTO response = aiCommandService.parseCommand(request, httpRequest);
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
    log.info("收到AI命令执行请求: {}", request.getFunctionCall().getName());
    try {
      Object result = aiCommandService.executeCommand(request, httpRequest);
      return Result.success(result);
    } catch (Exception e) {
      log.error("命令执行失败", e);
      return Result.failed(e.getMessage());
    }
  }

  @Operation(summary = "获取AI命令记录分页列表")
  @GetMapping("/records")
  public PageResult<AiCommandRecordVO> getRecordPage(AiCommandPageQuery queryParams) {
    IPage<AiCommandRecordVO> page = recordService.getRecordPage(queryParams);
    return PageResult.success(page);
  }

  @Operation(summary = "撤销命令执行")
  @PostMapping("/rollback/{recordId}")
  public Result<?> rollbackCommand(
    @Parameter(description = "记录ID") @PathVariable String recordId
  ) {
    recordService.rollbackCommand(recordId);
    return Result.success("撤销成功");
  }

}




