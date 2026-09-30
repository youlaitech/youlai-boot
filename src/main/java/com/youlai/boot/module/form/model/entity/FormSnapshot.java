package com.youlai.boot.module.form.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单版本快照实体
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName("form_snapshot")
@Data
public class FormSnapshot extends BaseEntity {

    // 表单定义ID
    private Long formId;

    // 版本号（发布时递增后的值）
    private Integer version;

    // 表单规则快照（form-create rule 数组 JSON 字符串）
    private String formJson;

    // 表单全局配置快照 JSON 字符串
    private String optionsJson;

    // 逻辑删除标识(0未删除 1已删除)
    private Integer isDeleted;

}
