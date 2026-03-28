package com.youlai.boot.auth.service;

import com.youlai.boot.framework.captcha.model.CaptchaInfo;
import com.youlai.boot.framework.security.model.AuthenticationToken;

/**
 * 认证服务接口
 *
 * @author Ray.Hao
 * @since 2.4.0
 */
public interface AuthService {

    /**
     * 账号密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证令牌
     */
    AuthenticationToken login(String username, String password);

    /**
     * 短信验证码登录
     *
     * @param mobile 手机号
     * @param code   验证码
     * @return 认证令牌
     */
    AuthenticationToken loginBySms(String mobile, String code);

    /**
     * 发送短信验证码
     *
     * @param mobile 手机号
     */
    void sendSmsCode(String mobile);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取验证码
     */
    CaptchaInfo getCaptcha();

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 认证令牌
     */
    AuthenticationToken refreshToken(String refreshToken);
}
