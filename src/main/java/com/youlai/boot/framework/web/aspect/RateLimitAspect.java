package com.youlai.boot.framework.web.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.youlai.boot.common.annotation.RateLimit;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.framework.web.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 * <p>对 @RateLimit 标注的方法做 Redis 计数，超阈值则拒绝</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Around("@annotation(rateLimit)")
    public Object handle(ProceedingJoinPoint jp, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String key = buildKey(request, rateLimit);

        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : rateLimitProperties.getDefaultLimit();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            int window = rateLimit.window() > 0 ? rateLimit.window() : rateLimitProperties.getDefaultWindow();
            redisTemplate.expire(key, window, rateLimit.timeUnit());
        }
        if (count != null && count > limit) {
            log.warn("限流触发 key={} count={} limit={}", key, count, limit);
            throw new BusinessException(ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED);
        }
        return jp.proceed();
    }

    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String user = resolveUser(request);
        return StrUtil.format(RedisConstants.RateLimiter.API, rateLimit.prefix(), user, request.getRequestURI());
    }

    private String resolveUser(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(header) && header.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return DigestUtil.sha256Hex(header.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length()));
        }
        return IPUtils.getIpAddr(request);
    }
}
