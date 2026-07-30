package com.youlai.boot.auth.enums;

/**
 * 扫码登录票据状态
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
public enum QrCodeLoginStatusEnum {

    /** 票据已创建，等待 APP 扫码 */
    WAITING,

    /** APP 已扫码，等待用户在手机上确认 */
    SCANNED,

    /** 用户已在 APP 上确认登录 */
    CONFIRMED,

    /** PC 已用票据换取会话令牌，票据作废，不可再用 */
    LOGGED_IN,

    /** 用户在 APP 上取消登录 */
    CANCELED,

    /** 票据超时，由 Redis TTL 自动清理 */
    EXPIRED
}
