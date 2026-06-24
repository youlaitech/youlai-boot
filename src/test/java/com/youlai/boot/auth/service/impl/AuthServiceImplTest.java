package com.youlai.boot.auth.service.impl;

import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.captcha.service.CaptchaService;
import com.youlai.boot.framework.integration.sms.enums.SmsTypeEnum;
import com.youlai.boot.framework.integration.sms.service.SmsService;
import com.youlai.boot.framework.security.token.TokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private SmsService smsService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CaptchaService captchaService;

    @Test
    void should_not_cache_sms_code_when_send_fails() {
        when(smsService.sendSms(eq("13800138000"), eq(SmsTypeEnum.LOGIN), any(Map.class))).thenReturn(false);

        AuthServiceImpl authService = new AuthServiceImpl(
                authenticationManager,
                tokenManager,
                smsService,
                redisTemplate,
                captchaService
        );

        assertThatThrownBy(() -> authService.sendSmsCode("13800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("短信验证码发送失败");

        verify(redisTemplate, never()).opsForValue();
    }
}
