package com.youlai.boot.auth.qrcode.model;

/**
 * 扫码登录票据状态。
 * <p>
 * WAITING   票据已创建，等待 APP 扫码
 * SCANNED   APP 已扫码，等待用户在手机上确认
 * CONFIRMED 用户已在 APP 上确认登录
 * LOGGED_IN PC 已用票据换取会话令牌，票据作废，不可再用
 * CANCELED  用户在 APP 上取消登录
 * EXPIRED   票据超时，由 Redis TTL 自动清理，内存中通常不会出现该值
 */
public enum QrCodeLoginStatusEnum {

    WAITING,
    SCANNED,
    CONFIRMED,
    LOGGED_IN,
    CANCELED,
    EXPIRED
}
