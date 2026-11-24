package com.youlai.boot.system.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 访问趋势查询对象
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
@Schema(description = "访问趋势查询对象")
@Getter
@Setter
public class VisitTrendQuery {

    private static final long MAX_RANGE_DAYS = 366;

    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期 yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期 yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 日期顺序与跨度校验
     */
    @AssertTrue(message = "结束日期不能早于开始日期，且跨度不能超过 366 天")
    @Schema(hidden = true)
    public boolean isValidRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return days >= 0 && days < MAX_RANGE_DAYS;
    }
}
