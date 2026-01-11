package com.youlai.boot.security.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * 微信小程序Code认证Token
 *
 * @author 有来技术团队
 * @since 2.0.0
 */
public class WxMiniAppCodeAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 621L;
    private final Object principal;

    /**
     * 微信小程序Code认证Token (未认证)
     *
     * @param principal 微信code
     */
    public WxMiniAppCodeAuthenticationToken(Object principal) {
        // 没有授权信息时，设置为 null
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        // 默认未认证
        this.setAuthenticated(false);
    }


    /**
     * 微信小程序Code认证Token (已认证)
     *
     * @param principal   微信用户信息
     * @param authorities 授权信息
     */
    public WxMiniAppCodeAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        // 认证通过
        super.setAuthenticated(true);
    }


    /**
     * 认证通过
     *
     * @param principal   微信用户信息
     * @param authorities 授权信息
     * @return 已认证的Token
     */
    public static WxMiniAppCodeAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new WxMiniAppCodeAuthenticationToken(principal, authorities);
    }

    @Override
    public Object getCredentials() {
        // 微信认证不需要密码
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
} 