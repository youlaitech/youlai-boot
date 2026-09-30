package com.youlai.boot.framework.web.ratelimit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.exception.RateLimitException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口级限流切面
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    @Around("@annotation(rateLimit)")
    public Object handle(ProceedingJoinPoint jp, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String key = buildKey(request, rateLimit);
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : rateLimitProperties.getDefaultLimit();
        long windowMs = rateLimit.window() > 0
                ? rateLimit.window() * 1000L
                : rateLimitProperties.getDefaultWindow().toMillis();

        // limiter 内部已统一 Fail-Open：Redis 异常返回 null，按未计数放行
        Long count = rateLimiter.count(key, windowMs);

        int current = count != null ? count.intValue() : 0;
        setRateLimitHeaders(request, limit, current, windowMs);

        if (current > limit) {
            log.warn("接口限流触发  key={}  count={}  limit={}", key, current, limit);
            throw new RateLimitException(ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED);
        }

        return jp.proceed();
    }

    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String user = resolveUser(request);
        return StrUtil.format(RedisConstants.RateLimit.API, user, request.getRequestURI());
    }

    private String resolveUser(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(header) && header.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return DigestUtil.sha256Hex(header.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length()));
        }
        return IPUtils.getIpAddr(request);
    }

    private void setRateLimitHeaders(HttpServletRequest request,
                                     int limit,
                                     int current,
                                     long windowMs) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletResponse response = attrs.getResponse();
            if (response != null) {
                int remaining = Math.max(0, limit - current);
                long resetAt = (System.currentTimeMillis() + windowMs) / 1000;
                response.setHeader("X-RateLimit-Limit",     String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Reset",     String.valueOf(resetAt));
            }
        }
    }

}
