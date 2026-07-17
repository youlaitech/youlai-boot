package com.youlai.boot.auth.qrcode.service;

import com.youlai.boot.auth.qrcode.model.vo.QrCodeGenerateVO;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeStatusVO;
import com.youlai.boot.framework.security.model.AuthenticationToken;

/**
 * 扫码登录服务。
 */
public interface QrCodeLoginService {

    /** 生成票据，未登录调用 */
    QrCodeGenerateVO generate(String clientIp);

    /** 查询状态，未登录调用 */
    QrCodeStatusVO status(String ticket);

    /** APP 标记已扫码，需 APP 端已登录 */
    QrCodeStatusVO scan(String ticket, Long userId);

    /** APP 确认登录，需 APP 端已登录 */
    QrCodeStatusVO confirm(String ticket, Long userId);

    /** APP 取消登录，需 APP 端已登录 */
    QrCodeStatusVO cancel(String ticket, Long userId);

    /** PC 端用票据换取会话令牌，未登录调用 */
    AuthenticationToken login(String ticket);
}
