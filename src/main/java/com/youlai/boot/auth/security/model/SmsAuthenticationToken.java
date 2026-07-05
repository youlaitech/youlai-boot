package com.youlai.boot.auth.security.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.Serial;
import java.util.Collection;

/**
 * 短信验证码认证 Token。
 * <p>
 * 未认证：principal=手机号，credentials=验证码；已认证：principal=SecurityUserDetails，credentials=null。
 *
 * @author Ray.Hao
 * @since 2.20.0
 */
public class SmsAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 621L;

    private final Object principal;
    private final Object credentials;

    public SmsAuthenticationToken(String mobile, String verifyCode) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.principal = mobile;
        this.credentials = verifyCode;
        setAuthenticated(false);
    }

    public SmsAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        super.setAuthenticated(true);
    }

    public static SmsAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new SmsAuthenticationToken(principal, authorities);
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
