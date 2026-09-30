package com.youlai.boot.auth.wxma.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 微信小程序登录时手机号未绑定异常。
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
public class MobileNotBoundException extends AuthenticationException {

    private final String openid;

    private final String sessionKey;

    public MobileNotBoundException(String openid, String sessionKey) {
        super("需要绑定手机号");
        this.openid = openid;
        this.sessionKey = sessionKey;
    }

    public String getOpenid() {
        return openid;
    }

    public String getSessionKey() {
        return sessionKey;
    }
}
