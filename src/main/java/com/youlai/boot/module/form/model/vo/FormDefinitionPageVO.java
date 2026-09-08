package com.youlai.boot.module.form.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 表单定义分页VO
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单定义分页对象")
@Getter
@Setter
public class FormDefinitionPageVO {

    @Schema(description = "表单ID")
    private Long id;

    @Schema(description = "表单唯一标识")
    private String formKey;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单描述")
    private String description;

    @Schema(description = "状态(0草稿 1已发布 -1已停用)")
    private Integer status;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否允许匿名公开访问(0否 1是)")
    private Integer isPublic;

    @Schema(description = "表单类型(normal通用表单 workflow审批表单)")
    private String category;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
