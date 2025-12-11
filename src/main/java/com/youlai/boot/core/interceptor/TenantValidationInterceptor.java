package com.youlai.boot.core.interceptor;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 租户ID强制校验拦截器
 * <p>
 * 对于需要租户隔离的接口，强制要求携带有效的租户ID
 * 防止恶意用户通过不携带租户ID来访问默认租户数据
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true")
public class TenantValidationInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;

    /**
     * 白名单路径：这些路径不需要租户ID校验
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/captcha",
            "/api/v1/tenant/list",
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/favicon.ico",
            "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String requestPath = request.getRequestURI();

        // 检查是否在白名单中
        if (isWhitelistPath(requestPath)) {
            return true;
        }

        // 检查租户ID是否存在
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("请求路径 {} 缺少租户ID", requestPath);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\":\"%s\",\"msg\":\"租户ID不能为空，请联系管理员\"}",
                    ResultCode.BAD_REQUEST.getCode()
            ));
            return false;
        }

        // 可选：校验租户是否有效（需要注入 TenantService）
        // 这里暂时只校验租户ID不为空

        return true;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelistPath(String requestPath) {
        return WHITELIST_PATHS.stream()
                .anyMatch(requestPath::startsWith);
    }
}
