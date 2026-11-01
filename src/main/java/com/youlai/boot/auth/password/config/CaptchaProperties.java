package com.youlai.boot.auth.password.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码配置属性；默认使用圆圈干扰的算术验证码，可按需覆盖
 *
 * @author Ray.Hao
 * @since 2023/11/24
 */
@Component
@ConfigurationProperties(prefix = "captcha")
@Data
public class CaptchaProperties {

    /** 验证码类型  circle-圆圈干扰验证码|gif-Gif验证码|line-干扰线验证码|shear-扭曲干扰验证码 */
    private String type = "circle";

    /** 验证码图片宽度 */
    private int width = 120;
    /** 验证码图片高度 */
    private int height = 40;

    /** 干扰线数量 */
    private int interfereCount = 2;

    /** 文本透明度 */
    private Float textAlpha = 0.8F;

    /** 验证码过期时间，单位：秒 */
    private Long expireSeconds = 120L;

    /** 验证码字符配置 */
    private CodeProperties code = new CodeProperties();

    /** 验证码字体 */
    private FontProperties font = new FontProperties();

    /**
     * 验证码字符配置
     */
    @Data
    public static class CodeProperties {
        /** 验证码字符类型 math-算术|random-随机字符串 */
        private String type = "math";
        /** 验证码字符长度，type=算术时，表示运算位数(1:个位数 2:十位数)；type=随机字符时，表示字符个数 */
        private int length = 1;
    }

    /**
     * 验证码字体配置
     */
    @Data
    public static class FontProperties {
        /** 字体名称 */
        private String name = "SansSerif";
        /** 字体样式  0-普通|1-粗体|2-斜体 */
        private int weight = 1;
        /** 字体大小 */
        private int size = 24;
    }


}
