package com.youlai.boot.module.form.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.youlai.boot.common.base.IBaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 表单状态枚举
 *
 * <p>正数为有效生命周期（草稿/已发布），-1 为退役态（已停用），
 * 与字典等模块"启停"语义（1/0）不同类，故独立定义</p>
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Schema(enumAsRef = true)
@Getter
public enum FormStatusEnum implements IBaseEnum<Integer> {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    DISABLED(-1, "已停用");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String label;

    FormStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

}
