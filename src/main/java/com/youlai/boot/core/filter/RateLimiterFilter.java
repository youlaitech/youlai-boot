package com.youlai.boot.core.filter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.core.web.WebResponseWriter;
import com.youlai.boot.system.service.ConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * IP 限流过滤器
 *
 * @author Theo
 * @since 2024/08/10 14:38
 */
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConfigService configService;

    private static final long DEFAULT_IP_LIMIT = 10L; // 默认 IP 限流阈值

    public RateLimiterFilter(RedisTemplate<String, Object> redisTemplate, ConfigService configService) {
        this.redisTemplate = redisTemplate;
        this.configService = configService;
    }

    /**
     * 判断 IP 是否触发限流
     * 默认限制同一 IP 每秒最多请求 10 次，可通过系统配置调整。
     * 如果系统未配置限流阈值，默认跳过限流。
     *
     * @param ip IP 地址
     * @return 是否限流：true 表示限流；false 表示未限流
     */
    public boolean rateLimit(String ip) {
        // 限流 Redis 键
        String key = StrUtil.format(RedisConstants.RateLimiter.IP, ip);

        // 自增请求计数
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null || count == 1) {
            // 第一次访问时设置过期时间为 1 秒
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }

        // 获取系统配置的限流阈值
        Object systemConfig = configService.getSystemConfig(SystemConstants.SYSTEM_CONFIG_IP_QPS_LIMIT_KEY);
        if (systemConfig == null) {
            // 系统未配置限流，跳过限流逻辑
            log.warn("系统未配置限流阈值，跳过限流");
            return false;
        }

        // 转换系统配置为限流值，默认为 10
        long limit = Convert.toLong(systemConfig, DEFAULT_IP_LIMIT);
        return count != null && count > limit;
    }

    /**
     * 执行 IP 限流逻辑
     * 如果 IP 请求超出限制，直接返回限流响应；否则继续执行过滤器链。
     *
     * @param request     请求体
     * @param response    响应体
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        // 获取请求的 IP 地址
        String ip = IPUtils.getIpAddr(request);

        // 判断是否限流
        if (rateLimit(ip)) {
            // 返回限流错误信息
            WebResponseWriter.writeError(response, ResultCode.REQUEST_CONCURRENCY_LIMIT_EXCEEDED);
            return;
        }

        // 未触发限流，继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
