package com.youlai.boot.system.security.adapter;

import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 权限查询适配器。
 * <p>
 * 实现 framework 层的 {@link PermissionPort}，委托 {@link RoleMenuService} 查询权限集合。
 *
 * @see PermissionPort
 */
@Component
@RequiredArgsConstructor
public class PermissionAdapter implements PermissionPort {

    private final RoleMenuService roleMenuService;

    @Override
    public Set<String> getRolePerms(Set<String> roleCodes) {
        return roleMenuService.getRolePermsByRoleCodes(roleCodes);
    }
}
