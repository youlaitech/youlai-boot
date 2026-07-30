package com.youlai.boot.auth.model.dto;

import lombok.Data;

/**
 * 扫码登录票据上下文，序列化为 JSON 存入 Redis
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Data
public class QrCodeLoginContext {

    /** 票据，UUID 无连字符 */
    private String ticket;

    /** 状态枚举名，取 {@link com.youlai.boot.auth.enums.QrCodeLoginStatusEnum#name()} */
    private String status;

    /** 扫码用户 ID，scan 时写入 */
    private Long userId;

    /** 用户原始昵称（未脱敏），status 接口返回时脱敏 */
    private String nickname;

    /** 用户头像 URL */
    private String avatar;

    /** 创建时间戳（毫秒） */
    private Long createdAt;

    /** 扫码时间戳（毫秒） */
    private Long scannedAt;

    /** 确认时间戳（毫秒） */
    private Long confirmedAt;

    /** generate 时的 PC 端 IP */
    private String clientIp;
}
