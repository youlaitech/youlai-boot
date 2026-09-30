package com.youlai.boot.auth.sms.service;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.sms.token.SmsAuthenticationToken;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.support.sms.SmsTypeEnum;
import com.youlai.boot.support.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码登录服务
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsAuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;

    private final SmsService smsService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送登录短信验证码
     *
     * @param mobile 手机号
     */
    public void sendSmsCode(String mobile) {

        // TODO 固定验证码 1234 便于测试，接入短信厂商后改为随机生成
        String code = "1234";

        // 发送短信验证码
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("code", code);
        try {
            smsService.send(mobile, SmsTypeEnum.LOGIN, templateParams);
        } catch (Exception e) {
            log.error("发送短信验证码失败", e);
        }
        // 缓存验证码至Redis，用于登录校验
        redisTemplate.opsForValue().set(StrUtil.format(RedisConstants.Captcha.SMS_LOGIN_CODE, mobile), code, 5, TimeUnit.MINUTES);
    }

    /**
     * 短信验证码登录
     *
     * @param mobile 手机号
     * @param code   验证码
     * @return 访问令牌
     */
    public AuthenticationToken loginBySms(String mobile, String code) {
        // 1. 创建用户短信验证码认证的令牌（未认证）
        SmsAuthenticationToken smsAuthenticationToken = new SmsAuthenticationToken(mobile, code);

        // 2. 执行认证（认证中）
        Authentication authentication = authenticationManager.authenticate(smsAuthenticationToken);

        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken authenticationToken = tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authenticationToken;
    }

}
