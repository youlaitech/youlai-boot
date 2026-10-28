package com.youlai.boot.auth.oidc.handler;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.auth.oidc.service.OidcTicketService;
import com.youlai.boot.framework.security.session.LoginContext;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.system.service.UserIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 处理 OIDC 登录结果并回跳前端
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Slf4j
@Component
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final OidcProperties oidcProperties;
    private final UserIdentityService userIdentityService;
    private final OidcTicketService oidcTicketService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
                redirect(response, Map.of("error", ResultCode.OIDC_LOGIN_FAILED.getCode()));
                return;
            }
            String provider = oidcProperties.getProvider();
            String subject = oidcUser.getSubject();
            Long userId = userIdentityService.findUserId(provider, subject);
            if (userId == null) {
                // 用户可将 subject 交给管理员完成身份关联
                redirect(response, Map.of("error", ResultCode.OIDC_ACCOUNT_NOT_BOUND.getCode(), "subject", subject));
                return;
            }
            var context = LoginContext.oidc(provider, oidcUser.getIssuer().toString(), subject,
                    oidcUser.getIdToken().getTokenValue());
            String ticket = oidcTicketService.issue(userId, context, request, response);
            redirect(response, Map.of("ticket", ticket));
        } finally {
            clearAuthentication(request);
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        clearAuthentication(request);
        log.warn("OIDC 登录失败：{}", exception.getClass().getSimpleName());
        redirect(response, Map.of("error", ResultCode.OIDC_LOGIN_FAILED.getCode()));
    }

    /**
     * 清理授权流程的临时会话，业务登录由票据兑换完成
     */
    private void clearAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 回跳前端，查询参数附在路由后
     */
    private void redirect(HttpServletResponse response, Map<String, String> parameters) throws IOException {
        String callbackUri = oidcProperties.getFrontendCallbackUri();
        if (StringUtils.isBlank(callbackUri)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未配置统一认证回跳地址");
            return;
        }

        StringBuilder url = new StringBuilder(callbackUri);
        // hash 路由的查询参数位于 # 后面
        String route = callbackUri.substring(callbackUri.indexOf('#') + 1);
        String separator = route.contains("?") ? "&" : "?";
        for (var parameter : parameters.entrySet()) {
            url.append(separator)
                    .append(parameter.getKey())
                    .append('=')
                    .append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8));
            separator = "&";
        }
        response.sendRedirect(url.toString());
    }
}
