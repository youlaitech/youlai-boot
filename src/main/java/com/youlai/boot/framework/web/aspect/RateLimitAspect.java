package com.youlai.boot.framework.web.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.youlai.boot.common.annotation.RateLimit;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.framework.web.config.RateLimitProperties;
import com.youlai.boot.framework.web.exception.RateLimitException;
import com.youlai.boot.framework.web.ratelimit.SlidingWindowScript;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * 接口级限流切面
 * <p>
 * 对标注了 {@link RateLimit} 的 Controller 方法做 Redis 滑动窗口计数。
 * 超阈值抛出 {@link BusinessException}（A0502），全局异常处理器统一返回 JSON。
 * </p>
 *
 * <h3>升级历史</h3>
 * <ul>
 *   <li>4.3.1 — 初始版本：固定窗口计数器</li>
 *   <li>4.4.0 — 升级为滑动窗口 + X-RateLimit-* 渐进式响应头</li>
 * </ul>
 *
 * @author Ray.Hao
 * @since 4.3.1
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
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String key = buildKey(request, rateLimit);
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : rateLimitProperties.getDefaultLimit();
        int window = rateLimit.window() > 0 ? rateLimit.window() : rateLimitProperties.getDefaultWindowSeconds();
        long windowMs = rateLimit.timeUnit().toMillis(window);

        Long count = SlidingWindowScript.execute(redisTemplate, key, windowMs);

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
        return StrUtil.format(RedisConstants.RateLimit.API,
                rateLimit.prefix(), user, request.getRequestURI());
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
