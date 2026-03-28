package com.youlai.boot.framework.captcha.service;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.enums.CaptchaTypeEnum;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.captcha.config.CaptchaProperties;
import com.youlai.boot.framework.captcha.exception.CaptchaException;
import com.youlai.boot.framework.captcha.model.CaptchaInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.Font;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;
    private final CodeGenerator codeGenerator;
    private final Font captchaFont;

    /**
     * 生成验证码
     */
    public CaptchaInfo generate() {
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

        String captchaId = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId),
                captchaCode,
                captchaProperties.getExpireSeconds(),
                TimeUnit.SECONDS
        );

        return CaptchaInfo.builder()
                .captchaId(captchaId)
                .captchaBase64(imageBase64Data)
                .build();
    }

    /**
     * 校验验证码，失败抛异常
     *
     * @param captchaId   验证码ID
     * @param captchaCode 用户输入的验证码
     * @throws CaptchaException 验证码错误或过期
     */
    public void validate(String captchaId, String captchaCode) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_ERROR);
        }

        String cacheKey = StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId);
        String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_EXPIRED);
        }

        if (!codeGenerator.verify(cachedCode, captchaCode)) {
            throw new CaptchaException(ResultCode.USER_VERIFICATION_CODE_ERROR);
        }

        redisTemplate.delete(cacheKey);
    }

}
