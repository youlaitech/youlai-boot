package com.youlai.boot.security.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * 短信验证码认证 Token
 * <p>
 * 用于短信验证码登录场景，遵循 Spring Security 认证模型：
 * <ul>
 *   <li>未认证状态：principal 为手机号，credentials 为验证码</li>
 *   <li>已认证状态：principal 为用户详情，credentials 为 null</li>
 * </ul>
 *
 * @author Ray.Hao
 * @since 2.20.0
 */
public class SmsAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 621L;

    /**
     * 认证信息
     * <ul>
     *   <li>未认证时：手机号</li>
     *   <li>已认证时：SysUserDetails 用户详情</li>
     * </ul>
     */
    private final Object principal;

    /**
     * 凭证信息
     * <ul>
     *   <li>未认证时：短信验证码</li>
     *   <li>已认证时：null</li>
     * </ul>
     */
    private final Object credentials;

    /**
     * 创建未认证的 Token
     *
     * @param mobile      手机号
     * @param verifyCode  短信验证码
     */
    public SmsAuthenticationToken(String mobile, String verifyCode) {
        super(null);
        this.principal = mobile;
        this.credentials = verifyCode;
        setAuthenticated(false);
    }

    /**
     * 创建已认证的 Token
     *
     * @param principal   用户详情（SysUserDetails）
     * @param authorities 授权信息
     */
    public SmsAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        super.setAuthenticated(true);
    }

    /**
     * 创建已认证的 Token（静态工厂方法）
     *
     * @param principal   用户详情（SysUserDetails）
     * @param authorities 授权信息
     * @return 已认证的 SmsAuthenticationToken
     */
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
