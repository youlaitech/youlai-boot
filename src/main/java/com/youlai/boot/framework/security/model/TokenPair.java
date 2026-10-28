package com.youlai.boot.framework.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录与刷新共用的令牌响应
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Schema(description = "登录令牌")
public record TokenPair(
        @Schema(description = "令牌类型", example = "Bearer") String tokenType,
        @Schema(description = "访问令牌") String accessToken,
        @Schema(description = "刷新令牌") String refreshToken,
        @Schema(description = "访问令牌剩余有效期，单位秒") long expiresIn) {

    /**
     * 创建 Bearer 令牌响应，有效期单位为秒
     */
    public static TokenPair bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenPair("Bearer", accessToken, refreshToken, expiresIn);
    }

    @Override
    public String toString() {
        return "TokenPair[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", credentials=REDACTED]";
    }
}
