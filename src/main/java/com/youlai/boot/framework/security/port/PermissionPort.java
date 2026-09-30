package com.youlai.boot.framework.security.port;

import java.util.Set;

/**
 * 权限查询端口。
 *
 * @see com.youlai.boot.system.adapter.security.PermissionAdapter
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
