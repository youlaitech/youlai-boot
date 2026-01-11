package com.youlai.boot.security.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

/**
 * 微信小程序手机号认证Token
 *
 * @author 有来技术团队
 * @since 2.0.0
 */
public class WxMiniAppPhoneAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 622L;
    
    private final Object principal; // code
    private String encryptedData;
    private String iv;

    /**
     * 微信小程序手机号认证Token (未认证)
     *
     * @param code 微信登录code
     * @param encryptedData 加密数据
     * @param iv 初始向量
     */
    public WxMiniAppPhoneAuthenticationToken(String code, String encryptedData, String iv) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = code;
        this.encryptedData = encryptedData;
        this.iv = iv;
        this.setAuthenticated(false);
    }

    /**
     * 微信小程序手机号认证Token (已认证)
     *
     * @param principal 用户信息
     * @param authorities 授权信息
     */
    public WxMiniAppPhoneAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    /**
     * 认证通过
     *
     * @param principal 用户信息
     * @param authorities 授权信息
     * @return 认证通过的Token
     */
    public static WxMiniAppPhoneAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new WxMiniAppPhoneAuthenticationToken(principal, authorities);
    }

    @Override
    public Object getCredentials() {
        // 微信小程序手机号认证不需要密码
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * 获取加密数据
     * 
     * @return 加密数据
     */
    public String getEncryptedData() {
        return encryptedData;
    }

    /**
     * 获取初始向量
     * 
     * @return 初始向量
     */
    public String getIv() {
        return iv;
    }
} 
