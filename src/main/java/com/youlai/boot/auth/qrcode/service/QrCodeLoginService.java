package com.youlai.boot.auth.qrcode.service;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.qrcode.enums.QrCodeLoginStatusEnum;
import com.youlai.boot.auth.qrcode.model.context.QrCodeLoginContext;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeGenerateVO;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeStatusVO;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.exception.TokenInvalidException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.session.SessionService;
import com.youlai.boot.framework.security.token.SecureTokenGenerator;
import com.youlai.boot.system.service.UserIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 扫码登录服务
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeLoginService {

    // 票据默认有效期（秒）
    private static final int DEFAULT_EXPIRE_SECONDS = 300;

    // 状态只能单向推进，写入沿用票据剩余有效期
    private static final DefaultRedisScript<Long> TRANSITION = new DefaultRedisScript<>("""
            local raw = redis.call('GET', KEYS[1])
            if not raw or cjson.decode(raw).status ~= ARGV[1] then return 0 end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl <= 0 then return 0 end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ttl)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper json;
    private final SessionService sessionService;
    private final UserIdentityService userIdentityService;

    /**
     * 生成扫码票据，写入 Redis 后返回 ticket 和有效期
     */
    public QrCodeGenerateVO generate(String clientIp) {
        String ticket = SecureTokenGenerator.generate();
        QrCodeLoginContext ctx = new QrCodeLoginContext();
        ctx.setTicket(ticket);
        ctx.setStatus(QrCodeLoginStatusEnum.WAITING.name());
        ctx.setCreatedAt(System.currentTimeMillis());
        ctx.setClientIp(clientIp);
        redisTemplate.opsForValue().set(key(ticket), json.writeValueAsString(ctx),
                Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS));
        return QrCodeGenerateVO.builder()
                .ticket(ticket)
                .expireSeconds(DEFAULT_EXPIRE_SECONDS)
                .build();
    }

    /**
     * PC 端轮询扫码状态，未登录调用
     */
    public QrCodeStatusVO status(String ticket) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        return toResp(ctx, remainingSeconds(ticket));
    }

    /**
     * APP 端标记已扫码，需已登录。WAITING → SCANNED
     */
    public QrCodeStatusVO scan(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.WAITING);
        fillUserInfo(ctx, userId);
        ctx.setStatus(QrCodeLoginStatusEnum.SCANNED.name());
        ctx.setScannedAt(System.currentTimeMillis());
        transition(ctx, QrCodeLoginStatusEnum.WAITING);
        return toResp(ctx, remainingSeconds(ticket));
    }

    /**
     * APP 端确认登录，需扫码本人操作。SCANNED → CONFIRMED
     */
    public QrCodeStatusVO confirm(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.SCANNED);
        requireSameUser(ctx, userId);
        ctx.setStatus(QrCodeLoginStatusEnum.CONFIRMED.name());
        ctx.setConfirmedAt(System.currentTimeMillis());
        transition(ctx, QrCodeLoginStatusEnum.SCANNED);
        return toResp(ctx, remainingSeconds(ticket));
    }

    /**
     * APP 端取消登录，仅流程中的票据可取消
     */
    public QrCodeStatusVO cancel(String ticket, Long userId) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        QrCodeLoginStatusEnum current = QrCodeLoginStatusEnum.valueOf(ctx.getStatus());
        // 只有还在流程中的票据（等待扫码 / 已扫码 / 已确认）允许取消
        if (current != QrCodeLoginStatusEnum.WAITING
                && current != QrCodeLoginStatusEnum.SCANNED
                && current != QrCodeLoginStatusEnum.CONFIRMED) {
            throw new TokenInvalidException(ResultCode.QR_CODE_STATUS_ILLEGAL);
        }
        // 一旦有人扫过码，取消权就归扫码本人
        if (current != QrCodeLoginStatusEnum.WAITING && ctx.getUserId() != null) {
            requireSameUser(ctx, userId);
        }
        ctx.setStatus(QrCodeLoginStatusEnum.CANCELED.name());
        transition(ctx, current);
        return toResp(ctx, remainingSeconds(ticket));
    }

    /**
     * PC 端用票据换取会话令牌，CONFIRMED → LOGGED_IN（一次性）
     */
    public TokenPair login(String ticket) {
        QrCodeLoginContext ctx = loadOrThrow(ticket);
        requireStatus(ctx, QrCodeLoginStatusEnum.CONFIRMED);
        SecurityUser securityUser = userIdentityService.getAuthInfoByUserId(ctx.getUserId());
        if (securityUser == null) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(securityUser.getStatus())) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_FROZEN);
        }
        SecurityUserDetails details = new SecurityUserDetails(securityUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        // 先原子消费，再签发凭证，确保并发兑换最多成功一次
        ctx.setStatus(QrCodeLoginStatusEnum.LOGGED_IN.name());
        transition(ctx, QrCodeLoginStatusEnum.CONFIRMED);
        return sessionService.create(authentication);
    }

    /**
     * 读取票据上下文，票据为空、不存在或已过期都视为 QR_CODE_NOT_FOUND
     */
    private QrCodeLoginContext loadOrThrow(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) {
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
        String raw = redisTemplate.opsForValue().get(key(ticket));
        if (raw == null) {
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
        try {
            return json.readValue(raw, QrCodeLoginContext.class);
        } catch (tools.jackson.core.JacksonException exception) {
            log.warn("扫码票据数据格式错误");
            throw new TokenInvalidException(ResultCode.QR_CODE_NOT_FOUND);
        }
    }

    /**
     * 当前状态必须等于期望状态，否则说明流程被跳步或重复操作
     */
    private void requireStatus(QrCodeLoginContext ctx, QrCodeLoginStatusEnum expected) {
        if (!expected.name().equals(ctx.getStatus())) {
            throw new TokenInvalidException(ResultCode.QR_CODE_STATUS_ILLEGAL);
        }
    }

    /**
     * 操作者必须是当初扫码的那个用户，防止 A 扫码 B 确认
     */
    private void requireSameUser(QrCodeLoginContext ctx, Long userId) {
        if (ctx.getUserId() == null || !ctx.getUserId().equals(userId)) {
            throw new TokenInvalidException(ResultCode.QR_CODE_USER_MISMATCH);
        }
    }

    /**
     * 扫码时把当前 APP 用户的昵称、头像写进上下文，供 PC 端 status 展示
     */
    private void fillUserInfo(QrCodeLoginContext ctx, Long userId) {
        SecurityUser info = userIdentityService.getAuthInfoByUserId(userId);
        if (info == null) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        ctx.setUserId(userId);
        ctx.setNickname(info.getNickname());
        ctx.setAvatar(info.getAvatar());
    }

    /**
     * 按期望状态原子替换，保持原有截止时间
     */
    private void transition(QrCodeLoginContext context, QrCodeLoginStatusEnum expected) {
        Long changed = redisTemplate.execute(TRANSITION, List.of(key(context.getTicket())),
                expected.name(), json.writeValueAsString(context));
        if (!Long.valueOf(1).equals(changed)) {
            throw new TokenInvalidException(ResultCode.QR_CODE_STATUS_ILLEGAL);
        }
    }

    /**
     * 票据在 Redis 中的剩余秒数，取不到或已过期返回 0
     */
    private int remainingSeconds(String ticket) {
        Long ttl = redisTemplate.getExpire(key(ticket), TimeUnit.SECONDS);
        return ttl == null ? 0 : Math.max(ttl.intValue(), 0);
    }

    /**
     * 上下文转前端 VO，昵称脱敏、用户信息仅在扫码后暴露
     */
    private QrCodeStatusVO toResp(QrCodeLoginContext ctx, int expireSeconds) {
        QrCodeLoginStatusEnum status = QrCodeLoginStatusEnum.valueOf(ctx.getStatus());
        String nickname = null;
        String avatar = null;
        // WAITING 阶段谁都能查状态，此时不能泄露用户信息；扫码/确认后才回传脱敏昵称与头像
        if (status == QrCodeLoginStatusEnum.SCANNED
                || status == QrCodeLoginStatusEnum.CONFIRMED) {
            nickname = maskNickname(ctx.getNickname());
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

    /**
     * 昵称脱敏：保留首尾各一个字符，中间用 * 替换
     */
    private String maskNickname(String nickname) {
        if (StrUtil.isBlank(nickname)) {
            return "";
        }
        int len = nickname.length();
        if (len == 1) {
            return nickname;
        }
        return StrUtil.hide(nickname, 1, len - 1);
    }

    /**
     * 拼接票据在 Redis 中的完整 Key
     */
    private String key(String ticket) {
        return StrUtil.format(RedisConstants.Auth.QR_CODE_LOGIN, SecureTokenGenerator.digest(ticket));
    }
}
