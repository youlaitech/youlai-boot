package com.youlai.boot.security.model;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.youlai.boot.common.constant.SecurityConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Spring Security 用户认证对象
 * <p>
 * 封装了用户的基本信息和权限信息，供 Spring Security 进行用户认证与授权。
 * 实现了 {@link UserDetails} 接口，提供用户的核心信息。
 *
 * @author Ray.Hao
 * @version 3.0.0
 */
@Data
@NoArgsConstructor
public class SysUserDetails implements UserDetails {

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
     * 数据权限范围
     */
    private Integer dataScope;

    /**
     * 用户角色权限集合
     */
    private Collection<SimpleGrantedAuthority> authorities;

    /**
     * 构造函数：根据用户认证信息初始化用户详情对象
     *
     * @param user 用户认证信息对象 {@link UserAuthCredentials}
     */
    public SysUserDetails(UserAuthCredentials user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = ObjectUtil.equal(user.getStatus(), 1);
        this.deptId = user.getDeptId();
        this.dataScope = user.getDataScope();

        // 初始化角色权限集合
        this.authorities = CollectionUtil.isNotEmpty(user.getRoles())
                ? user.getRoles().stream()
                // 角色名加上前缀 "ROLE_"，用于区分角色 (ROLE_ADMIN) 和权限 (user:add)
                .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
                .collect(Collectors.toSet())
                : Collections.emptySet();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
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
}
