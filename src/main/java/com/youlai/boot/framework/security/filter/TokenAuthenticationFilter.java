package com.youlai.boot.framework.security.filter;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.framework.security.token.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 认证过滤器。
 *
 * @author wangtao
 * @since 2025/3/6
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;

    public TokenAuthenticationFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawToken = resolveToken(request);

        if (StrUtil.isNotBlank(rawToken)) {
            try {
                boolean isValid = tokenManager.validateToken(rawToken);
                if (!isValid) {
                    SecurityContextHolder.clearContext();
                    throw new InsufficientAuthenticationException("Token 无效或已过期");
                }
                Authentication authentication = tokenManager.parseToken(rawToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                throw ex;
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                throw new InsufficientAuthenticationException("Token 认证失败", ex);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(header) && header.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length());
        }
        return null;
    }
}
