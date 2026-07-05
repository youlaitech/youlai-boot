package com.youlai.boot.framework.security.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import java.util.Set;

/**
 * Spring Security 权限校验组件。
 * <p>
 * 用于 SpEL 表达式：{@code @PreAuthorize("@ss.hasPerm('sys:user:create')")}。
 * 通过 {@link PermissionPort} 查询角色权限，不直接依赖 system 模块。
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Component("ss")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionPort permissionPort;

    /**
     * 判断当前用户是否拥有操作权限，支持通配符匹配。
     */
    public boolean hasPerm(String requiredPerm) {
        if (StrUtil.isBlank(requiredPerm)) {
            return false;
        }

        if (SecurityUtils.isRoot()) {
            return true;
        }

        Set<String> roleCodes = SecurityUtils.getRoles();
        if (CollectionUtil.isEmpty(roleCodes)) {
            return false;
        }

        Set<String> rolePerms = permissionPort.getRolePerms(roleCodes);
        if (CollectionUtil.isEmpty(rolePerms)) {
            return false;
        }

        boolean hasPermission = rolePerms.stream()
                .anyMatch(rolePerm -> PatternMatchUtils.simpleMatch(rolePerm, requiredPerm));

        if (!hasPermission) {
            log.warn("用户无操作权限：userId={}, username={}, requiredPerm={}",
                    SecurityUtils.getUserId(), SecurityUtils.getUsername(), requiredPerm);
        }
        return hasPermission;
    }

}
