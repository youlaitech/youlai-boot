package com.youlai.boot.framework.security.port;

import java.util.Set;

/**
 * 权限查询端口。
 * <p>
 * 由 system 模块提供适配器实现，framework 层通过此接口获取角色权限集合，
 * 不直接依赖 system 模块的 {@code RoleMenuService}。
 *
 * @see com.youlai.boot.system.security.adapter.PermissionAdapter
 */
public interface PermissionPort {

    /**
     * 根据角色编码集合查询权限标识集合。
     *
     * @param roleCodes 角色编码集合
     * @return 权限标识集合，如 "sys:user:create"
     */
    Set<String> getRolePerms(Set<String> roleCodes);
}
