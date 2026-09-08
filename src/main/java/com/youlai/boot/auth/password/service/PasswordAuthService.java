package com.youlai.boot.auth.password.service;

import com.youlai.boot.auth.password.model.vo.CaptchaVO;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.token.TokenManager;
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
    private final TokenManager tokenManager;
    private final CaptchaService captchaService;

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 访问令牌
     */
    public AuthenticationToken login(String username, String password) {
        // 1. 创建用于密码认证的令牌（未认证）
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username.trim(), password);

        // 2. 执行认证（认证中）
        // 说明：这里的认证流程由 Spring Security 提供的 AuthenticationManager 执行。
        // 默认情况下会委托给 DaoAuthenticationProvider：
        // 1) retrieveUser(...)：内部通过 UserDetailsService.loadUserByUsername(...) 获取用户信息（本项目为 SecurityUserDetailsService 实现）
        // 2) additionalAuthenticationChecks(...)：对比请求密码与用户存储密码（由 PasswordEncoder 完成匹配）
        // 认证通过后返回已认证的 Authentication（principal 为 SecurityUserDetails，authorities 为角色/权限集合）。
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken authenticationTokenResponse =
                tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authenticationTokenResponse;
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
