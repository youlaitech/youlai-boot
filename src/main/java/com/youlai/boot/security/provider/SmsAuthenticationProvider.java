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
 * <p>
 * 实现 Spring Security 的 {@link AuthenticationProvider} 接口，处理短信验证码登录认证。
 * <p>
 * 认证流程：
 * <ol>
 *   <li>根据手机号查询用户信息</li>
 *   <li>校验用户状态（是否禁用）</li>
 *   <li>校验短信验证码（与 Redis 缓存比对）</li>
 *   <li>验证成功后删除验证码，防止重复使用</li>
 *   <li>返回已认证的 Authentication</li>
 * </ol>
 *
 * @author Ray.Hao
 * @since 2.17.0
 * @see SmsAuthenticationToken
 * @see AuthenticationProvider
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
     * 执行短信验证码认证
     *
     * @param authentication 未认证的 {@link SmsAuthenticationToken}
     * @return 已认证的 {@link SmsAuthenticationToken}
     * @throws AuthenticationException 认证失败异常
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String mobile = (String) authentication.getPrincipal();
        String inputVerifyCode = (String) authentication.getCredentials();

        // 参数校验
        if (StrUtil.isBlank(mobile)) {
            log.warn("短信验证码登录失败：手机号为空");
            throw new CaptchaValidationException("手机号不能为空");
        }
        if (StrUtil.isBlank(inputVerifyCode)) {
            log.warn("短信验证码登录失败：验证码为空，手机号={}", mobile);
            throw new CaptchaValidationException("验证码不能为空");
        }

        // 根据手机号获取用户信息
        UserAuthInfo userAuthInfo = userService.getAuthInfoByMobile(mobile);

        if (userAuthInfo == null) {
            log.warn("短信验证码登录失败：用户不存在，手机号={}", mobile);
            throw new UsernameNotFoundException("用户不存在");
        }

        // 检查用户状态是否有效
        if (ObjectUtil.notEqual(userAuthInfo.getStatus(), 1)) {
            log.warn("短信验证码登录失败：用户已禁用，用户名={}", userAuthInfo.getUsername());
            throw new DisabledException("用户已被禁用");
        }

        // 校验短信验证码
        String cacheKey = StrUtil.format(RedisConstants.Captcha.SMS_LOGIN_CODE, mobile);
        String cachedVerifyCode = (String) redisTemplate.opsForValue().get(cacheKey);

        if (cachedVerifyCode == null) {
            log.warn("短信验证码登录失败：验证码已过期，手机号={}", mobile);
            throw new CaptchaValidationException("验证码已过期，请重新获取");
        }

        if (!StrUtil.equals(inputVerifyCode, cachedVerifyCode)) {
            log.warn("短信验证码登录失败：验证码错误，手机号={}", mobile);
            throw new CaptchaValidationException("验证码错误");
        }

        // 验证成功后删除验证码，防止重复使用
        redisTemplate.delete(cacheKey);

        // 构建认证后的用户详情信息
        SysUserDetails userDetails = new SysUserDetails(userAuthInfo);

        log.info("短信验证码登录成功：用户名={}，手机号={}", userAuthInfo.getUsername(), mobile);

        // 创建已认证的 SmsAuthenticationToken
        return SmsAuthenticationToken.authenticated(userDetails, userDetails.getAuthorities());
    }

    /**
     * 支持的认证类型
     *
     * @param authentication 认证类型
     * @return 是否支持该认证类型
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return SmsAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
