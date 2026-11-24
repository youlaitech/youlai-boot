package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单日访问量
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
@Schema(description = "单日访问量")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VisitTrendPointVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "浏览量")
    private Integer pv;

    @Schema(description = "访客数")
    private Integer uv;
}
