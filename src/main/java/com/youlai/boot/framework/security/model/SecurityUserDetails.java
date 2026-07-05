package com.youlai.boot.framework.security.model;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.youlai.boot.common.constant.SecurityConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Security 用户认证对象。
 * <p>
 * 实现 {@link UserDetails}，封装用户标识、角色、数据权限等认证信息。
 * {@link #roles} 字段存储角色编码（不带 ROLE_ 前缀），
 * {@link #getAuthorities()} 运行时补前缀并转为 {@link SimpleGrantedAuthority}。
 *
 * @author Ray.Hao
 * @version 3.0.0
 */
@Data
@NoArgsConstructor
public class SecurityUserDetails implements UserDetails {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 账号是否启用(true:启用 false:禁用)
     */
    private Boolean enabled;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 数据权限列表
     */
    private List<RoleDataScope> dataScopes;

    /**
     * 角色编码集合（不带 ROLE_ 前缀）
     */
    private Set<String> roles;

    /**
     * 构造函数：根据 {@link SecurityUser} 初始化。
     */
    public SecurityUserDetails(SecurityUser user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = ObjectUtil.equal(user.getStatus(), 1);
        this.deptId = user.getDeptId();
        this.dataScopes = user.getDataScopes();
        this.roles = user.getRoles();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (CollectionUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * 判断是否包含"全部数据"权限
     */
    public boolean hasAllDataScope() {
        if (CollectionUtil.isEmpty(dataScopes)) {
            return false;
        }
        return dataScopes.stream()
                .anyMatch(scope -> scope.getDataScope() == 1);
    }

    /**
     * 获取数据权限列表，永不为 null
     */
    public List<RoleDataScope> getDataScopes() {
        return dataScopes != null ? dataScopes : Collections.emptyList();
    }
}
