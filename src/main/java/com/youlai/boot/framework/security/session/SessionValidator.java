package com.youlai.boot.framework.security.session;

/**
 * 外部登录来源的会话校验
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public interface SessionValidator {

    /**
     * 本校验器负责的登录来源
     */
    LoginContext.Source source();

    /**
     * 校验本实现负责的外部登录来源，失效时拒绝认证
     */
    void validate(LoginSession session);
}
