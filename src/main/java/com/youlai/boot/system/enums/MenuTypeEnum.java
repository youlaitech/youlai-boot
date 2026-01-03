package com.youlai.boot.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.youlai.boot.common.base.IBaseEnum;
import lombok.Getter;

/**
 * 菜单类型枚举（char）
 *
 * C：目录
 * M：菜单
 * B：按钮
 */
@Getter
public enum MenuTypeEnum implements IBaseEnum<String> {

    CATALOG("C", "目录"),
    MENU("M", "菜单"),
    BUTTON("B", "按钮");

    /**
     * 数据库存储值
     */
    @EnumValue
    private final String value;

    /**
     * 友好名称
     */
    private final String label;

    MenuTypeEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

}
