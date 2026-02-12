package com.youlai.boot.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 角色数据权限信息
 * <p>
 * 用于存储单个角色的数据权限范围信息，支持多角色数据权限合并（并集策略）
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 数据权限范围值
     * 1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据
     */
    private Integer dataScope;

    /**
     * 自定义部门ID列表（仅当 dataScope=5 时有效）
     */
    private List<Long> customDeptIds;

    /**
     * 创建"全部数据"权限
     */
    public static RoleDataScope all(String roleCode) {
        return new RoleDataScope(roleCode, 1, null);
    }

    /**
     * 创建"部门及子部门"权限
     */
    public static RoleDataScope deptAndSub(String roleCode) {
        return new RoleDataScope(roleCode, 2, null);
    }

    /**
     * 创建"本部门"权限
     */
    public static RoleDataScope dept(String roleCode) {
        return new RoleDataScope(roleCode, 3, null);
    }

    /**
     * 创建"本人"权限
     */
    public static RoleDataScope self(String roleCode) {
        return new RoleDataScope(roleCode, 4, null);
    }

    /**
     * 创建"自定义部门"权限
     */
    public static RoleDataScope custom(String roleCode, List<Long> deptIds) {
        return new RoleDataScope(roleCode, 5, deptIds);
    }

}
