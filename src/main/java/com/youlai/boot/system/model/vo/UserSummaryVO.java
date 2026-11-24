package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户汇总视图对象
 *
 * @author Ray.Hao
 * @since 4.15.0
 */
@Schema(description = "用户汇总视图对象")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryVO {

    @Schema(description = "用户总数")
    private Long total;
}
