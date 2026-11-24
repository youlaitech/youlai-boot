package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 访问趋势视图对象
 *
 * @author Ray.Hao
 * @since 2.3.0
 */
@Schema(description = "访问趋势视图对象")
@Getter
@Setter
public class VisitTrendVO {

    @Schema(description = "查询开始日期 yyyy-MM-dd")
    private String startDate;

    @Schema(description = "查询结束日期 yyyy-MM-dd")
    private String endDate;

    @Schema(description = "逐日访问量")
    private List<VisitTrendPointVO> points;
}
