package com.youlai.boot.security.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.system.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import java.util.Set;

/**
 * Spring Security 权限校验组件
 * <p>
 * 用于 SpEL 表达式权限校验，如：@PreAuthorize("@ss.hasPerm('sys:user:add')")
 * <p>
 * 权限数据来源：{@link RoleMenuService#getRolePermsByRoleCodes}（带 Redis 缓存）
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Component("ss")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final RoleMenuService roleMenuService;

    /**
     * 判断当前登录用户是否拥有操作权限
     * <p>
     * 支持通配符匹配，如：权限码 "sys:user:*" 可匹配 "sys:user:add"、"sys:user:delete" 等
     *
     * @param requiredPerm 所需权限
     * @return 是否有权限
     */
    public boolean hasPerm(String requiredPerm) {
        if (StrUtil.isBlank(requiredPerm)) {
            return false;
        }

        // 超级管理员放行
        if (SecurityUtils.isRoot()) {
            return true;
        }

        // 获取当前登录用户的角色编码集合
        Set<String> roleCodes = SecurityUtils.getRoles();
        if (CollectionUtil.isEmpty(roleCodes)) {
            return false;
        }

        // 获取当前登录用户的所有角色的权限列表（从缓存读取）
        Set<String> rolePerms = roleMenuService.getRolePermsByRoleCodes(roleCodes);
        if (CollectionUtil.isEmpty(rolePerms)) {
            return false;
        }

        // 判断权限列表中是否包含所需权限（支持通配符）
        boolean hasPermission = rolePerms.stream()
                .anyMatch(rolePerm -> PatternMatchUtils.simpleMatch(rolePerm, requiredPerm));

        if (!hasPermission) {
            log.warn("用户无操作权限：userId={}, username={}, requiredPerm={}",
                    SecurityUtils.getUserId(), SecurityUtils.getUsername(), requiredPerm);
        }
        return hasPermission;
    }

}
