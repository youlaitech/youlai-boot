package com.youlai.boot.system.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置查询对象
 *
 * @author Theo
 * @since 2024-7-29 11:38:00
 */
@Getter
@Setter
@Schema(description = "系统配置查询")
public class ConfigQuery extends BaseQuery {

    @Schema(description="关键字(配置项名称/配置项值)")
    private String keywords;
}
