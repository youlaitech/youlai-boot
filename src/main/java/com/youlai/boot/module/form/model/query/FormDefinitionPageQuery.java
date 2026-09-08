package com.youlai.boot.module.form.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单定义分页查询对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "表单定义分页查询对象")
public class FormDefinitionPageQuery extends BaseQuery {

    @Schema(description = "关键字（表单名称/标识）")
    private String keywords;

    @Schema(description = "状态(0草稿 1已发布 -1已停用)")
    private Integer status;

    @Schema(description = "表单类型(normal通用表单 workflow审批表单)")
    private String category;

}
