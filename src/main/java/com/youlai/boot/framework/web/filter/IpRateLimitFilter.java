package com.youlai.boot.framework.web.filter;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.framework.web.config.RateLimitProperties;
import com.youlai.boot.framework.web.ratelimit.SlidingWindowScript;
import com.youlai.boot.framework.web.util.ResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * IP 全局限流过滤器
 * <p>
 * 在所有请求进入 Controller 之前，按 IP 维度做滑动窗口限流。
 * 与 {@code @RateLimit} 注解级限流叠加：先过 IP 全局，再过接口级。
 * </p>
 *
 * <h3>限流维度</h3>
 * <pre>{@code
 *   Key: rate_limit:ip:{clientIp}
 *   默认: 1000 req / 60s（可通过 rate-limit.ip.* 配置）
 * }</pre>
 *
 * <h3>响应头</h3>
 * <table>
 *   <tr><td>X-RateLimit-Limit</td><td>窗口内最大允许请求数</td></tr>
 *   <tr><td>X-RateLimit-Remaining</td><td>窗口内剩余可用请求数</td></tr>
 *   <tr><td>X-RateLimit-Reset</td><td>窗口重置时间（Unix 秒）</td></tr>
 *   <tr><td>Retry-After</td><td>超限时建议重试等待秒数（仅 429）</td></tr>
 * </table>
 *
 * @author Ray.Hao
 * @since 4.4.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
@Slf4j
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
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
        long windowMs = ipConfig.getWindowSeconds() * 1000L;

        // 执行滑动窗口计数（Lua 原子操作）
        Long count = SlidingWindowScript.execute(redisTemplate, key, windowMs);

        int limit = ipConfig.getLimit();
        int current = count != null ? count.intValue() : 0;

        setRateLimitHeaders(response, limit, current, windowMs);

        if (current > limit) {
            log.warn("IP 限流触发  ip={}  count={}  limit={}", ip, current, limit);
            response.setHeader("Retry-After", String.valueOf(ipConfig.getWindowSeconds()));
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
