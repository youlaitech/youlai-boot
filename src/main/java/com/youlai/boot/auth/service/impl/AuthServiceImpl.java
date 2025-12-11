package com.youlai.boot.auth.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.model.dto.WxMiniAppCodeLoginDTO;
import com.youlai.boot.auth.model.dto.WxMiniAppPhoneLoginDTO;
import com.youlai.boot.auth.model.vo.CaptchaVO;
import com.youlai.boot.auth.service.AuthService;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.enums.CaptchaTypeEnum;
import com.youlai.boot.config.property.CaptchaProperties;
import com.youlai.boot.platform.sms.enums.SmsTypeEnum;
import com.youlai.boot.platform.sms.service.SmsService;
import com.youlai.boot.security.model.AuthenticationToken;
import com.youlai.boot.security.model.SmsAuthenticationToken;
import com.youlai.boot.security.model.WxMiniAppCodeAuthenticationToken;
import com.youlai.boot.security.model.WxMiniAppPhoneAuthenticationToken;
import com.youlai.boot.security.token.TokenManager;
import com.youlai.boot.security.util.SecurityUtils;
import com.youlai.boot.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author Ray.Hao
 * @since 2.4.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;

    private final Font captchaFont;
    private final CaptchaProperties captchaProperties;
    private final CodeGenerator codeGenerator;

    private final SmsService smsService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @param tenantId 租户ID（可选，多租户模式下用于指定租户）
     * @return 访问令牌
     */
    @Override
    public AuthenticationToken login(String username, String password, Long tenantId) {
        // 如果指定了租户ID，需要先设置租户上下文，以便查询该租户下的用户
        if (tenantId != null) {
            com.youlai.boot.common.tenant.TenantContextHolder.setTenantId(tenantId);
        }
        
        // 1. 创建用于密码认证的令牌（未认证）
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username.trim(), password);

        // 2. 执行认证（认证中）
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken authenticationTokenResponse =
                tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authenticationTokenResponse;
    }

    /**
     * 微信一键授权登录
     *
     * @param code 微信登录code
     * @return 访问令牌
     */
    @Override
    public AuthenticationToken loginByWechat(String code) {
        // 1. 创建用户微信认证的令牌（未认证）
        WxMiniAppCodeAuthenticationToken authenticationToken = new WxMiniAppCodeAuthenticationToken(code);

        // 2. 执行认证（认证中）
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken token = tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return token;
    }

    /**
     * 发送登录短信验证码
     *
     * @param mobile 手机号
     */
    @Override
    public void sendSmsLoginCode(String mobile) {

        // 随机生成4位验证码
        // String code = String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
        // TODO 为了方便测试，验证码固定为 1234，实际开发中在配置了厂商短信服务后，可以使用上面的随机验证码
        String code = "1234";

        // 发送短信验证码
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("code", code);
        try {
            smsService.sendSms(mobile, SmsTypeEnum.LOGIN, templateParams);
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
    @Override
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

    /**
     * 注销登录
     */
    @Override
    public void logout() {
        String token = SecurityUtils.getAccessToken();
        if (StrUtil.isNotBlank(token)) {
            tokenManager.invalidateToken(token);
            // 清除Security上下文
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 获取验证码
     *
     * @return 验证码
     */
    @Override
    public CaptchaVO getCaptcha() {

        String captchaType = captchaProperties.getType();
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int interfereCount = captchaProperties.getInterfereCount();
        int codeLength = captchaProperties.getCode().getLength();

        AbstractCaptcha captcha;
        if (CaptchaTypeEnum.CIRCLE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createCircleCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.GIF.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createGifCaptcha(width, height, codeLength);
        } else if (CaptchaTypeEnum.LINE.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        } else if (CaptchaTypeEnum.SHEAR.name().equalsIgnoreCase(captchaType)) {
            captcha = CaptchaUtil.createShearCaptcha(width, height, codeLength, interfereCount);
        } else {
            throw new IllegalArgumentException("Invalid captcha type: " + captchaType);
        }
        captcha.setGenerator(codeGenerator);
        captcha.setTextAlpha(captchaProperties.getTextAlpha());
        captcha.setFont(captchaFont);

        String captchaCode = captcha.getCode();
        String imageBase64Data = captcha.getImageBase64Data();

        // 验证码文本缓存至Redis，用于登录校验
        String captchaId = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId),
                captchaCode,
                captchaProperties.getExpireSeconds(),
                TimeUnit.SECONDS
        );

        return CaptchaVO.builder()
                .captchaId(captchaId)
                .captchaBase64(imageBase64Data)
                .build();
    }

    /**
     * 刷新token
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @Override
    public AuthenticationToken refreshToken(String refreshToken) {
        return tokenManager.refreshToken(refreshToken);
    }

    /**
     * 微信小程序Code登录
     *
     * @param loginDTO 登录参数
     * @return 访问令牌
     */
    @Override
    public AuthenticationToken loginByWxMiniAppCode(WxMiniAppCodeLoginDTO loginDTO) {
        // 1. 创建微信小程序认证令牌（未认证）
        WxMiniAppCodeAuthenticationToken authenticationToken = new WxMiniAppCodeAuthenticationToken(loginDTO.getCode());

        // 2. 执行认证（认证中）
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 认证成功后生成 JWT 令牌，并存入 Security 上下文，供登录日志 AOP 使用（已认证）
        AuthenticationToken token = tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return token;
    }

    /**
     * 微信小程序手机号登录
     *
     * @param loginDTO 登录参数
     * @return 访问令牌
     */
    @Override
    public AuthenticationToken loginByWxMiniAppPhone(WxMiniAppPhoneLoginDTO loginDTO) {
        // 创建微信小程序手机号认证Token
        WxMiniAppPhoneAuthenticationToken authenticationToken = new WxMiniAppPhoneAuthenticationToken(
                loginDTO.getCode(),
                loginDTO.getEncryptedData(),
                loginDTO.getIv()
        );

        // 执行认证
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 认证成功后生成JWT令牌，并存入Security上下文
        AuthenticationToken token = tokenManager.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return token;
    }

}
