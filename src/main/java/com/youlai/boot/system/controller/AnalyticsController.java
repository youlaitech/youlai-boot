package com.youlai.boot.system.controller;

import com.youlai.boot.common.result.Result;
import com.youlai.boot.system.model.query.VisitTrendQuery;
import com.youlai.boot.system.model.vo.UserSummaryVO;
import com.youlai.boot.system.model.vo.VisitSummaryVO;
import com.youlai.boot.system.model.vo.VisitTrendVO;
import com.youlai.boot.system.service.LogService;
import com.youlai.boot.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制层
 *
 * <p>访问统计按服务器配置的时区以自然日聚合，统计区间为闭区间。</p>
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
@Tag(name = "10.统计接口")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final LogService logService;
    private final UserService userService;

    /**
     * 访问汇总
     */
    @Operation(summary = "访问汇总")
    @GetMapping("/visits/summary")
    public Result<VisitSummaryVO> getVisitSummary() {
        return Result.success(logService.getVisitSummary());
    }

    /**
     * 访问趋势
     */
    @Operation(summary = "访问趋势")
    @GetMapping("/visits/trend")
    public Result<VisitTrendVO> getVisitTrend(@Valid VisitTrendQuery queryParams) {
        return Result.success(logService.getVisitTrend(queryParams.getStartDate(), queryParams.getEndDate()));
    }

    /**
     * 用户汇总
     */
    @Operation(summary = "用户汇总")
    @GetMapping("/users/summary")
    public Result<UserSummaryVO> getUserSummary() {
        return Result.success(new UserSummaryVO(userService.count()));
    }
}
