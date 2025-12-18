package com.youlai.boot.security.provider;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.security.exception.CaptchaValidationException;
import com.youlai.boot.security.model.SmsAuthenticationToken;
import com.youlai.boot.security.model.SysUserDetails;
import com.youlai.boot.security.model.UserAuthInfo;
import com.youlai.boot.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 短信验证码认证 Provider
 *
 * @author Ray.Hao
 * @since 2.17.0
 */
@Slf4j
public class SmsAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;


    public SmsAuthenticationProvider(UserService userService, RedisTemplate<String, Object> redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 短信验证码认证逻辑，参考 Spring Security 认证密码校验流程
     *
     * @param authentication 认证对象
     * @return 认证后的 Authentication 对象
     * @throws AuthenticationException 认证异常
     * @see org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider#authenticate(Authentication)
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String mobile = (String) authentication.getPrincipal();
        String inputVerifyCode = (String) authentication.getCredentials();

        // 根据手机号获取用户信息
        UserAuthInfo userAuthInfo = userService.getAuthInfoByMobile(mobile);

        if (userAuthInfo == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 检查用户状态是否有效
        if (ObjectUtil.notEqual(userAuthInfo.getStatus(), 1)) {
            throw new DisabledException("用户已被禁用");
        }

        // 校验发送短信验证码的手机号是否与当前登录用户一致
        String cacheKey = StrUtil.format(RedisConstants.Captcha.SMS_LOGIN_CODE, mobile);
        String cachedVerifyCode = (String) redisTemplate.opsForValue().get(cacheKey);

        if (!StrUtil.equals(inputVerifyCode, cachedVerifyCode)) {
            throw new CaptchaValidationException("验证码错误");
        } else {
            // 验证成功后删除验证码
            redisTemplate.delete(cacheKey);
        }

        // 构建认证后的用户详情信息
        SysUserDetails userDetails = new SysUserDetails(userAuthInfo);

        // 创建已认证的 SmsAuthenticationToken
        return SmsAuthenticationToken.authenticated(
                userDetails,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
