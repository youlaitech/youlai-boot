package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 访问汇总视图对象
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
@Schema(description = "访问汇总视图对象")
@Getter
@Setter
public class VisitSummaryVO {

    @Schema(description = "今日访客数")
    private Integer todayUv;

    @Schema(description = "昨日访客数")
    private Integer yesterdayUv;

    @Schema(description = "累计访客数")
    private Integer totalUv;

    @Schema(description = "今日浏览量")
    private Integer todayPv;

    @Schema(description = "昨日浏览量")
    private Integer yesterdayPv;

    @Schema(description = "累计浏览量")
    private Integer totalPv;

    @Schema(description = "访客增长率；昨日为 0 时为 null")
    private BigDecimal uvGrowthRate;

    @Schema(description = "浏览量增长率；昨日为 0 时为 null")
    private BigDecimal pvGrowthRate;
}
