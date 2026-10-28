package com.youlai.boot.auth.password.service;

import com.youlai.boot.auth.password.model.vo.CaptchaVO;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 账号密码登录服务
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Service
@RequiredArgsConstructor
public class PasswordAuthService {

    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;
    private final CaptchaService captchaService;

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 访问令牌
     */
    public TokenPair login(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username.trim(), password);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        TokenPair tokens = sessionService.create(authentication);
        // 登录日志从安全上下文读取操作者
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokens;
    }

    /**
     * 获取验证码
     *
     * @return 验证码ID与图片Base64
     */
    public CaptchaVO getCaptcha() {
        return captchaService.generate();
    }

}
