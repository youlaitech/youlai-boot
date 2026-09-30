package com.youlai.boot.module.form.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单数据实体
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName("form_data")
@Data
public class FormData extends BaseEntity {

    // 表单定义ID
    private Long formId;

    // 表单版本（提交时快照，用于追溯该数据由哪版规则产出）
    private Integer formVersion;

    // 表单数据（field -> value 映射的 JSON 字符串）
    private String dataJson;

    // 提交人ID（匿名提交为空）
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    // 修改人ID（数据修正时记录）
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // 逻辑删除标识(0未删除 1已删除)
    private Integer isDeleted;

}
