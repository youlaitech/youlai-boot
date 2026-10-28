package com.youlai.boot.framework.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 生成密码学随机凭证及其 SHA-256 摘要
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public final class SecureTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenGenerator() {
    }

    /**
     * 生成 32 字节随机值，并编码为无填充的 Base64URL
     */
    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算凭证的 SHA-256 十六进制摘要
     */
    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 以恒定时间比较凭证摘要
     */
    public static boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }
}
