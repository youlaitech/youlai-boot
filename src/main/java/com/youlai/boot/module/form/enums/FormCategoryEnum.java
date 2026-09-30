package com.youlai.boot.module.form.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.youlai.boot.common.base.IBaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 表单类型枚举
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(enumAsRef = true)
@Getter
public enum FormCategoryEnum implements IBaseEnum<String> {

    /**
     * 通用表单：公开收集 / 菜单入口，与流程无关
     */
    NORMAL("normal", "普通表单"),

    /**
     * 工作流表单：可被工作流绑定为发起 / 办理表单
     */
    WORKFLOW("workflow", "工作流表单");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    FormCategoryEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

}