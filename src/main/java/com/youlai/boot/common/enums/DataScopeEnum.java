package com.youlai.boot.common.enums;

import com.youlai.boot.common.base.IBaseEnum;
import lombok.Getter;

/**
 * 数据权限枚举
 * <p>
 * value 越小，数据权限范围越大。
 * 多角色数据权限合并策略：取并集（OR），即用户能看到所有角色权限范围内的数据。
 * 如果任一角色是 ALL，则直接跳过数据权限过滤。
 *
 * @author Ray.Hao
 * @since 2.3.0
 */
@Getter
public enum DataScopeEnum implements IBaseEnum<Integer> {

    /**
     * 所有数据权限 - 最高权限，可查看所有数据
     */
    ALL(1, "所有数据"),

    /**
     * 部门及子部门数据 - 可查看本部门及其下属所有部门的数据
     */
    DEPT_AND_SUB(2, "部门及子部门数据"),

    /**
     * 本部门数据 - 仅可查看本部门的数据
     */
    DEPT(3, "本部门数据"),

    /**
     * 本人数据 - 仅可查看自己的数据
     */
    SELF(4, "本人数据"),

    /**
     * 自定义部门数据 - 可查看指定部门的数据
     * <p>
     * 需要配合 sys_role_dept 表使用，存储角色可访问的部门ID列表
     */
    CUSTOM(5, "自定义部门数据");

    private final Integer value;

    private final String label;

    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 判断是否为全部数据权限
     *
     * @param value 数据权限值
     * @return 是否为全部数据权限
     */
    public static boolean isAll(Integer value) {
        return ALL.getValue().equals(value);
    }

    /**
     * 根据值获取枚举
     *
     * @param value 数据权限值
     * @return 枚举对象，未找到则返回 null
     */
    public static DataScopeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (DataScopeEnum dataScope : values()) {
            if (dataScope.getValue().equals(value)) {
                return dataScope;
            }
        }
        return null;
    }
}
