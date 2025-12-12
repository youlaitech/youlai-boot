package com.youlai.boot.security.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import com.youlai.boot.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import java.util.*;

/**
 * SpringSecurity 权限校验
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Component("ss")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TenantProperties tenantProperties;

    /**
     * 判断当前登录用户是否拥有操作权限
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

        // 获取当前登录用户的所有角色的权限列表
        Set<String> rolePerms = this.getRolePermsFormCache(roleCodes);
        if (CollectionUtil.isEmpty(rolePerms)) {
            return false;
        }
        // 判断当前登录用户的所有角色的权限列表中是否包含所需权限
        boolean hasPermission = rolePerms.stream()
                .anyMatch(rolePerm ->
                        // 匹配权限，支持通配符(* 等)
                        PatternMatchUtils.simpleMatch(rolePerm, requiredPerm)
                );

        if (!hasPermission) {
            log.error("用户无操作权限：{}",requiredPerm);
        }
        return hasPermission;
    }


    /**
     * 构建租户权限缓存key
     *
     * @param tenantId 租户ID
     * @return 缓存key
     */
    private String buildRolePermsCacheKey(Long tenantId) {
        if (!tenantProperties.getEnabled() || tenantId == null) {
            return RedisConstants.System.ROLE_PERMS;
        }
        return RedisConstants.System.ROLE_PERMS + ":" + tenantId;
    }

    /**
     * 从缓存中获取角色权限列表（兼容单租户和多租户）
     *
     * @param roleCodes 角色编码集合
     * @return 角色权限列表
     */
    public Set<String> getRolePermsFormCache(Set<String> roleCodes) {
        if (CollectionUtil.isEmpty(roleCodes)) {
            return Collections.emptySet();
        }

        // 获取当前租户ID并构建缓存Key
        Long tenantId = TenantContextHolder.getTenantId();
        String cacheKey = buildRolePermsCacheKey(tenantId);

        Set<String> perms = new HashSet<>();
        Collection<Object> roleCodesAsObjects = new ArrayList<>(roleCodes);
        List<Object> rolePermsList = redisTemplate.opsForHash().multiGet(cacheKey, roleCodesAsObjects);

        for (Object rolePermsObj : rolePermsList) {
            if (rolePermsObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> rolePerms = (Set<String>) rolePermsObj;
                perms.addAll(rolePerms);
            }
        }

        return perms;
    }

}
