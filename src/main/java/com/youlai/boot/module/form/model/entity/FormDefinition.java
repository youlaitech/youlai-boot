package com.youlai.boot.module.form.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单定义实体
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName("form_definition")
@Data
public class FormDefinition extends BaseEntity {

    // 表单唯一标识（业务编码，创建后不可修改）
    private String formKey;

    // 表单名称
    private String formName;

    // 表单描述
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    // 表单规则（form-create rule 数组 JSON 字符串）
    private String formJson;

    // 表单全局配置 JSON 字符串（labelPosition/size/submitBtn 等）
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String optionsJson;

    // 状态(0草稿 1已发布 -1已停用)
    private Integer status;

    // 是否允许匿名公开访问(0否 1是)
    private Integer isPublic;

    // 表单类型(normal通用表单 workflow审批表单)
    private String category;

    // 生成的访问菜单ID（NULL未生成，幂等防重复）
    private Long menuId;

    // 版本号，每次发布+1
    private Integer version;

    // 创建人ID
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    // 更新人ID
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // 逻辑删除标识(0未删除 1已删除)
    private Integer isDeleted;

}
