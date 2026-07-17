package com.youlai.boot.auth.qrcode.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.qrcode.model.QrCodeLoginContext;
import com.youlai.boot.auth.qrcode.model.QrCodeLoginStatusEnum;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeGenerateVO;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeStatusVO;
import com.youlai.boot.auth.qrcode.service.QrCodeLoginService;
import com.youlai.boot.auth.qrcode.util.QrCodeNicknameMasker;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.exception.TokenInvalidException;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

/**
 * 扫码登录服务实现。
 * <p>
 * 票据上下文以 JSON 形式存入 Redis，TTL 与有效期一致；状态流转的合法性全部由
 * {@link #requireStatus} / {@link #requireSameUser} 在写入前拦截，非法迁移直接抛业务异常，
 * 因此同一 ticket 被多端并发操作时不需要分布式锁——最坏情况是后到的操作在状态校验处失败。
 * PC 端换取会话时直接复用 {@link TokenManager#generateToken(Authentication)}，
 * 生成的令牌与账号密码登录走的是同一套会话治理（单/多设备、登出、刷新），前端无差别处理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeLoginServiceImpl implements QrCodeLoginService {

    /** 票据默认有效期（秒），二维码在此时间内未走完流程即由 Redis TTL 自动清理 */
    private static final int DEFAULT_EXPIRE_SECONDS = 300;

    /** 状态流转时若 Redis 剩余 TTL 小于该值，补足到此值，避免临界点票据在下一步操作前被清掉 */
    private static final int MIN_REMAIN_SECONDS = 30;

    /**
     * 从 Redis 读回的值可能是不带类型信息的 Map（序列化器以 Object.class 存储），
     * 用它把 Map 转回强类型上下文对象。JsonMapper 线程安全，复用单例。
     */
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenManager tokenManager;
    private final UserSocialService userSocialService;

    @Override
    public QrCodeGenerateVO generate(String clientIp) {
        String ticket = IdUtil.fastSimpleUUID();
        QrCodeLoginContext ctx = new QrCodeLoginContext();
        ctx.setTicket(ticket);
        ctx.setStatus(QrCodeLoginStatusEnum.WAITING.name());
        ctx.setCreatedAt(System.currentTimeMillis());
        ctx.setClientIp(clientIp);
        save(ctx, DEFAULT_EXPIRE_SECONDS);
        return QrCodeGenerateVO.builder()
                .ticket(ticket)
                .expireSeconds(DEFAULT_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public QrCodeStatusVO status(String ticket) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        return toVO(ctx, remainingSeconds(ticket));
    }

    @Override
    public QrCodeStatusVO scan(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.WAITING);
        fillUserInfo(ctx, userId);
        ctx.setStatus(QrCodeLoginStatusEnum.SCANNED.name());
        ctx.setScannedAt(System.currentTimeMillis());
        save(ctx, refreshTtl(ticket));
        return toVO(ctx, remainingSeconds(ticket));
    }

    @Override
    public QrCodeStatusVO confirm(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.SCANNED);
        requireSameUser(ctx, userId);
        ctx.setStatus(QrCodeLoginStatusEnum.CONFIRMED.name());
        ctx.setConfirmedAt(System.currentTimeMillis());
        save(ctx, refreshTtl(ticket));
        return toVO(ctx, remainingSeconds(ticket));
    }

    @Override
    public QrCodeStatusVO cancel(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        QrCodeLoginStatusEnum current = QrCodeLoginStatusEnum.valueOf(ctx.getStatus());
        // 只有还在流程中的票据（等待扫码 / 已扫码 / 已确认）允许取消，已登录或已取消的重复操作直接拒绝
        if (current != QrCodeLoginStatusEnum.WAITING
                && current != QrCodeLoginStatusEnum.SCANNED
                && current != QrCodeLoginStatusEnum.CONFIRMED) {
            throw new TokenInvalidException(ResultCode.QR_CODE_STATUS_ILLEGAL);
        }
        // 一旦有人扫过码，取消权就归扫码本人，防止他人替扫码用户取消
        if (current != QrCodeLoginStatusEnum.WAITING && ctx.getUserId() != null) {
            requireSameUser(ctx, userId);
        }
        ctx.setStatus(QrCodeLoginStatusEnum.CANCELED.name());
        save(ctx, refreshTtl(ticket));
        return toVO(ctx, remainingSeconds(ticket));
    }

    @Override
    public AuthenticationToken login(String ticket) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.CONFIRMED);
        SecurityUser securityUser = userSocialService.getAuthInfoByUserId(ctx.getUserId());
        if (securityUser == null) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        SecurityUserDetails details = new SecurityUserDetails(securityUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        AuthenticationToken token = tokenManager.generateToken(authentication);
        // 换取令牌成功后立即把票据置为已使用（一次性），再次 login 会在 requireStatus(CONFIRMED) 处被拒，杜绝重放
        ctx.setStatus(QrCodeLoginStatusEnum.LOGGED_IN.name());
        save(ctx, Math.max(remainingSeconds(ticket), MIN_REMAIN_SECONDS));
        return token;
    }

    // ======================== private ========================

    /** 读取票据上下文，票据为空、不存在或已过期都视为 QR_CODE_NOT_FOUND */
    private QrCodeLoginContext loadOrThrow(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
        Object raw = redisTemplate.opsForValue().get(key(ticket));
        if (raw == null) {
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
        if (raw instanceof QrCodeLoginContext ctx) {
            return ctx;
        }
        // 序列化器以 Object.class 存储、未带类型信息，读回来是 Map，这里再转回强类型
        try {
            return JSON_MAPPER.convertValue(raw, QrCodeLoginContext.class);
        } catch (Exception e) {
            log.error("扫码登录上下文反序列化失败, ticket={}", ticket, e);
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
    }

    /** 当前状态必须等于期望状态，否则说明流程被跳步或重复操作 */
    private void requireStatus(QrCodeLoginContext ctx, QrCodeLoginStatusEnum expected) {
        if (!expected.name().equals(ctx.getStatus())) {
            throw new TokenInvalidException(ResultCode.QR_CODE_STATUS_ILLEGAL);
        }
    }

    /** 操作者必须是当初扫码的那个用户，防止 A 扫码 B 确认 */
    private void requireSameUser(QrCodeLoginContext ctx, Long userId) {
        if (ctx.getUserId() == null || !ctx.getUserId().equals(userId)) {
            throw new TokenInvalidException(ResultCode.QR_CODE_USER_MISMATCH);
        }
    }

    /** 扫码时把当前 APP 用户的昵称、头像写进上下文，供 PC 端 status 展示 */
    private void fillUserInfo(QrCodeLoginContext ctx, Long userId) {
        SecurityUser info = userSocialService.getAuthInfoByUserId(userId);
        if (info == null) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        ctx.setUserId(userId);
        ctx.setNickname(info.getNickname());
        ctx.setAvatar(info.getAvatar());
    }

    /** 重写整个上下文并刷新 TTL */
    private void save(QrCodeLoginContext ctx, int ttl) {
        redisTemplate.opsForValue().set(key(ctx.getTicket()), ctx, ttl, TimeUnit.SECONDS);
    }

    /** 票据在 Redis 中的剩余秒数，取不到或已过期返回 0 */
    private int remainingSeconds(String ticket) {
        Long ttl = redisTemplate.getExpire(key(ticket), TimeUnit.SECONDS);
        return ttl == null ? 0 : Math.max(ttl.intValue(), 0);
    }

    /** 状态流转时写回的 TTL：维持剩余时间，不足 MIN_REMAIN_SECONDS 则补足 */
    private int refreshTtl(String ticket) {
        int remain = remainingSeconds(ticket);
        return remain < MIN_REMAIN_SECONDS ? MIN_REMAIN_SECONDS : remain;
    }

    /** 上下文转前端 VO，昵称脱敏、用户信息仅在扫码后暴露 */
    private QrCodeStatusVO toVO(QrCodeLoginContext ctx, int expireSeconds) {
        QrCodeLoginStatusEnum status = QrCodeLoginStatusEnum.valueOf(ctx.getStatus());
        String nickname = null;
        String avatar = null;
        // WAITING 阶段谁都能查状态，此时不能泄露用户信息；扫码/确认后才回传脱敏昵称与头像
        if (status == QrCodeLoginStatusEnum.SCANNED
                || status == QrCodeLoginStatusEnum.CONFIRMED) {
            nickname = QrCodeNicknameMasker.mask(ctx.getNickname());
            avatar = ctx.getAvatar();
        }
        return QrCodeStatusVO.builder()
                .ticket(ctx.getTicket())
                .status(status.name())
                .nickname(nickname)
                .avatar(avatar)
                .expireSeconds(expireSeconds)
                .build();
    }

    /** 拼接票据在 Redis 中的完整 Key */
    private String key(String ticket) {
        return StrUtil.format(RedisConstants.Auth.QR_CODE_LOGIN, ticket);
    }
}
