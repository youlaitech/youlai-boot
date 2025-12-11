package com.youlai.boot.core.filter;

import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import com.youlai.boot.security.model.SysUserDetails;
import com.youlai.boot.security.token.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TokenManager tokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1) 优先从已认证用户中获取租户ID
            Long tenantId = resolveTenantFromAuthentication(SecurityContextHolder.getContext().getAuthentication());

            // 2) 如果尚未获取到，尝试从 Token 中解析
            if (tenantId == null) {
                tenantId = resolveTenantFromToken(request);
            }

            // 3) 仍为空则使用默认租户
            if (tenantId == null) {
                Long defaultTenantId = tenantProperties.getDefaultTenantId();
                if (defaultTenantId != null) {
                    tenantId = defaultTenantId;
                }
            }

            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("TenantContextFilter set tenantId: {}", tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Long resolveTenantFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SysUserDetails details) {
            return details.getTenantId();
        }
        return null;
    }

    private Long resolveTenantFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
        Authentication authentication = tokenManager.parseToken(token);
        return resolveTenantFromAuthentication(authentication);
    }
}

