package com.youlai.boot.framework.web.filter;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.IPUtils;
import com.youlai.boot.framework.web.ratelimit.RateLimitProperties;
import com.youlai.boot.framework.web.ratelimit.RedisSlidingWindowRateLimiter;
import com.youlai.boot.framework.web.util.ResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * IP 全局限流过滤器
 *
 * @author Ray.Hao
 * @since 4.4.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
@Slf4j
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final RedisSlidingWindowRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 未启用时直接放行
        RateLimitProperties.Ip ipConfig = rateLimitProperties.getIp();
        if (!ipConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = IPUtils.getIpAddr(request);
        String key = StrUtil.format(RedisConstants.RateLimit.IP, ip);
        long windowMs = ipConfig.getWindow().toMillis();

        // limiter 内部已统一 Fail-Open：Redis 异常返回 null，按未计数放行
        Long count = rateLimiter.count(key, windowMs);

        int limit = ipConfig.getLimit();
        int current = count != null ? count.intValue() : 0;

        setRateLimitHeaders(response, limit, current, windowMs);

        if (current > limit) {
            log.warn("IP 限流触发  ip={}  count={}  limit={}", ip, current, limit);
            response.setHeader("Retry-After", String.valueOf(ipConfig.getWindow().getSeconds()));
            ResponseWriter.writeError(response, ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 设置渐进式限流响应头（X-RateLimit-Limit / Remaining / Reset）。
     * 超限时额外返回 Retry-After。
     */
    private void setRateLimitHeaders(HttpServletResponse response,
                                     int limit,
                                     int current,
                                     long windowMs) {
        int remaining = Math.max(0, limit - current);
        long resetAt = (System.currentTimeMillis() + windowMs) / 1000;
        response.setHeader("X-RateLimit-Limit",     String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(resetAt));
    }

}
