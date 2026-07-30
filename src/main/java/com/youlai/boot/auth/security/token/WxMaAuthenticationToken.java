package com.youlai.boot.auth.security.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.Serial;
import java.util.Collection;

/**
 * 微信小程序认证 Token。
 * <p>
 * 未认证：principal=微信code；已认证：principal=SecurityUserDetails。
 *
 * @author Ray.Hao
 * @since 4.0.0
 */
public class WxMaAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 622L;

    private final Object principal;
    private final Object credentials;

    public WxMaAuthenticationToken(String code) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.principal = code;
        this.credentials = null;
        setAuthenticated(false);
    }

    public WxMaAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        super.setAuthenticated(true);
    }

    public static WxMaAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new WxMaAuthenticationToken(principal, authorities);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
