package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 访问趋势Vo
 *
 * @author Ray.Hao
 * @since 2.3.0
 */
@Schema(description = "访问趋势Vo")
@Getter
@Setter
public class VisitTrendVO {

    @Schema(description = "日期列表")
    private List<String> dates;

    @Schema(description = "浏览量(PV)")
    private List<Integer> pvList;

    @Schema(description = "IP数")
    private List<Integer> ipList;
}

 