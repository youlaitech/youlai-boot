package com.youlai.boot.framework.security.util;

import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.framework.security.model.RoleDataScope;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

/**
 * Spring Security 工具类
 *
 * @author Ray
 * @since 2021/1/10
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户
     */
    public static Optional<SecurityUserDetails> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof SecurityUserDetails) {
                return Optional.of((SecurityUserDetails) principal);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return getUser().map(SecurityUserDetails::getUserId).orElse(null);
    }

    /**
     * 获取用户账号
     */
    public static String getUsername() {
        return getUser().map(SecurityUserDetails::getUsername).orElse(null);
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        return getUser().map(SecurityUserDetails::getDeptId).orElse(null);
    }

    /**
     * 获取数据权限列表
     */
    public static List<RoleDataScope> getDataScopes() {
        return getUser().map(SecurityUserDetails::getDataScopes).orElse(List.of());
    }

    /**
     * 获取角色编码集合，不带 ROLE_ 前缀。
     * <p>
     * 直接从 {@link SecurityUserDetails#getRoles()} 取值，不做 stripping 和过滤。
     */
    public static Set<String> getRoles() {
        return getUser().map(SecurityUserDetails::getRoles).orElse(Set.of());
    }

    /**
     * 是否超级管理员
     */
    public static boolean isRoot() {
        return getRoles().contains(SystemConstants.ROOT_ROLE_CODE);
    }

    /**
     * 从请求头获取 Token
     */
    public static String getAccessToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }

}
