package com.youlai.boot.common.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 微信小程序登录时手机号未绑定异常。
 * <p>
 * 携带 openid 和 sessionKey 供前端引导用户完成手机号绑定。
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
