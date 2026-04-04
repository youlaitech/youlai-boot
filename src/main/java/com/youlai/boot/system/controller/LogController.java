package com.youlai.boot.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.system.model.query.LogQuery;
import com.youlai.boot.system.model.vo.LogPageVO;
import com.youlai.boot.system.model.vo.VisitOverviewVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;
import com.youlai.boot.system.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 日志控制层
 *
 * @author Ray.Hao
 * @since 2.10.0
 */
@Tag(name = "09.日志接口")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @Operation(summary = "日志分页列表")
    @GetMapping
    @Log(module = LogModuleEnum.LOG, value = ActionTypeEnum.LIST)
    public PageResult<LogPageVO> getLogPage(
             LogQuery queryParams
    ) {
        Page<LogPageVO> result = logService.getLogPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "访问趋势统计")
    @GetMapping("/analytics/trend")
    public Result<VisitTrendVO> getVisitTrend(
            @Parameter(description = "开始时间", example = "2024-01-01") @RequestParam String startDate,
            @Parameter(description = "结束时间", example = "2024-12-31") @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        VisitTrendVO data = logService.getVisitTrend(start, end);
        return Result.success(data);
    }

    @Operation(summary = "访问统计概览")
    @GetMapping("/analytics/overview")
    public Result<VisitOverviewVO> getVisitOverview() {
        VisitOverviewVO result = logService.getVisitStats();
        return Result.success(result);
    }

}
