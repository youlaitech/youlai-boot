package com.youlai.boot.core.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.common.annotation.RepeatSubmit;
import com.youlai.boot.common.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * @since 2.3.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RepeatSubmitAspect {

    private final RedissonClient redissonClient;

    /**
     * 防重复提交切点
     */
    @Pointcut("@annotation(repeatSubmit)")
    public void repeatSubmitPointCut(RepeatSubmit repeatSubmit) {
    }

    /**
     * 环绕通知：处理防重复提交逻辑
     */
    @Around(value = "repeatSubmitPointCut(repeatSubmit)", argNames = "pjp,repeatSubmit")
    public Object handleRepeatSubmit(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey();

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
     * @return 锁的 key
     */
    private String buildLockKey() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 用户唯一标识
        String userIdentifier = getUserIdentifier(request);
        // 请求唯一标识 = 请求方法 + 请求路径 + 请求参数(严谨的做法)
        String requestIdentifier = StrUtil.join(":", request.getMethod(), request.getRequestURI());
        return StrUtil.format(RedisConstants.Lock.RESUBMIT, userIdentifier, requestIdentifier);
    }

    /**
     *  获取用户唯一标识
     *  1. 从请求头中获取 Token，使用 SHA-256 加密 Token 作为用户唯一标识
     *  2. 如果 Token 为空，使用 IP 作为用户唯一标识
     *
     * @param request 请求对象
     * @return 用户唯一标识
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // 用户身份唯一标识
        String userIdentifier;
        // 从请求头中获取 Token
        String tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(tokenHeader) && tokenHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            String rawToken = tokenHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());  // 去掉 Bearer 后的 Token
            userIdentifier = DigestUtil.sha256Hex(rawToken); // 使用 SHA-256 加密 Token 作为用户唯一标识
        } else {
            userIdentifier = IPUtils.getIpAddr(request); // 使用 IP 作为用户唯一标识
        }
        return userIdentifier;
    }


}

