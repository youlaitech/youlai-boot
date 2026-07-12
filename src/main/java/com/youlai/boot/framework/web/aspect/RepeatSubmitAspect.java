package com.youlai.boot.framework.web.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.annotation.RepeatSubmit;
import com.youlai.boot.common.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedissonClient redissonClient;

    @Pointcut("@annotation(repeatSubmit)")
    public void repeatSubmitPointCut(RepeatSubmit repeatSubmit) {
    }

    @Around(value = "repeatSubmitPointCut(repeatSubmit)", argNames = "pjp,repeatSubmit")
    public Object handleRepeatSubmit(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey(pjp);

        int expire = repeatSubmit.expire();
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = lock.tryLock(0, expire, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMISSION);
        }
        return pjp.proceed();
    }

    /**
     * 生成防重复提交锁的 key
     * <p>
     * key 由「用户标识 + 接口(method:URI) + 请求体哈希」组成：只有同一用户在同一接口提交
     * 完全相同的请求体才视为重复提交；若仅按 method:URI 生成，同一接口的不同提交
     * （如新增菜单与新增目录）会在窗口内被误判为重复。
     */
    private String buildLockKey(ProceedingJoinPoint pjp) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String userIdentifier = getUserIdentifier(request);
        String requestIdentifier = StrUtil.join(":", request.getMethod(), request.getRequestURI());
        String bodyHash = DigestUtil.sha256Hex(JSONUtil.toJsonStr(pjp.getArgs()));
        return StrUtil.format(RedisConstants.Lock.RESUBMIT, userIdentifier, requestIdentifier + ":" + bodyHash);
    }

    /**
     * 获取用户唯一标识
     * 1. 从请求头中获取 Token，使用 SHA-256 加密 Token 作为用户唯一标识
     * 2. 如果 Token 为空，使用 IP 作为用户唯一标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        String userIdentifier;
        String tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(tokenHeader) && tokenHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            String rawToken = tokenHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
            userIdentifier = DigestUtil.sha256Hex(rawToken);
        } else {
            userIdentifier = IPUtils.getIpAddr(request);
        }
        return userIdentifier;
    }

}
