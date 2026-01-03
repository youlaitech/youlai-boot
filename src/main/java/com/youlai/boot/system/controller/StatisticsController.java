package com.youlai.boot.system.controller;

import com.youlai.boot.core.web.Result;
import com.youlai.boot.system.model.vo.VisitStatsVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;
import com.youlai.boot.system.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 统计分析控制层
 *
 * @author Ray.Hao
 * @since 2025-12-15
 */
@Tag(name = "11.统计分析")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final LogService logService;

    @Operation(summary = "访问趋势统计")
    @GetMapping("/visits/trend")
    public Result<VisitTrendVO> getVisitTrend(
            @Parameter(description = "开始时间", example = "2024-01-01") @RequestParam String startDate,
            @Parameter(description = "结束时间", example = "2024-12-31") @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        VisitTrendVO data = logService.getVisitTrend(start, end);
        return Result.success(data);
    }

    @Operation(summary = "访问概览统计")
    @GetMapping("/visits/overview")
    public Result<VisitStatsVO> getVisitOverview() {
        VisitStatsVO result = logService.getVisitStats();
        return Result.success(result);
    }
}
