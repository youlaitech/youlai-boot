package com.youlai.boot.core.filter;

import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * <p>
 * 从请求头中获取租户ID，设置到线程上下文
 * 请求结束时自动清除上下文，避免线程池复用导致的数据泄露
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Slf4j
@Component
@Order(1) // 确保在其他过滤器之前执行
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 从请求头获取租户ID
            String tenantIdStr = request.getHeader(tenantProperties.getHeaderName());

            if (StringUtils.hasText(tenantIdStr)) {
                try {
                    Long tenantId = Long.parseLong(tenantIdStr);
                    TenantContextHolder.setTenantId(tenantId);
                    log.debug("从请求头获取租户ID: {}", tenantId);
                } catch (NumberFormatException e) {
                    log.warn("租户ID格式错误: {}", tenantIdStr);
                }
            } else {
                // 如果未提供租户ID，使用默认租户ID
                Long defaultTenantId = tenantProperties.getDefaultTenantId();
                if (defaultTenantId != null) {
                    TenantContextHolder.setTenantId(defaultTenantId);
                    log.debug("使用默认租户ID: {}", defaultTenantId);
                }
            }

            // 继续执行过滤器链
            filterChain.doFilter(request, response);

        } finally {
            // 请求结束时清除租户上下文，避免线程池复用导致的数据泄露
            TenantContextHolder.clear();
        }
    }
}

