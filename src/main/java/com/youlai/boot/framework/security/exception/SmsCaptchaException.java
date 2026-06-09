package com.youlai.boot.framework.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 短信验证码异常
 *
 * @author Ray.Hao
 * @since 2025/3/1
 */
public class SmsCaptchaException extends AuthenticationException {
    public SmsCaptchaException(String msg) {
        super(msg);
    }
}
