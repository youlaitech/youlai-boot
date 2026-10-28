package com.youlai.boot.auth.oidc.service;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.config.OidcClientRegistrationConfig;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.framework.security.session.LoginContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 为 OIDC 登录生成认证中心登出地址
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Service
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcLogoutService {

    private final OidcProperties oidcProperties;
    private final ClientRegistrationRepository clientRegistrations;

    /**
     * 本地登录或提供方不支持登出时返回 null
     */
    public String getLogoutUrl(LoginContext context) {
        if (context.source() != LoginContext.Source.OIDC) {
            return null;
        }

        var registration = clientRegistrations.findByRegistrationId(OidcClientRegistrationConfig.REGISTRATION_ID);
        String redirectUri = getPostLogoutRedirectUri();
        if (registration == null || redirectUri == null) {
            return null;
        }
        Object endpoint = registration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint");
        if (!(endpoint instanceof String logoutEndpoint) || StringUtils.isBlank(logoutEndpoint)) {
            // 提供方未声明主动登出能力时，只完成本地退出
            return null;
        }

        String separator = logoutEndpoint.contains("?") ? "&" : "?";
        String logoutUrl = logoutEndpoint + separator + "post_logout_redirect_uri="
                + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        if (StringUtils.isNotBlank(context.idToken())) {
            return logoutUrl + "&id_token_hint="
                    + URLEncoder.encode(context.idToken(), StandardCharsets.UTF_8);
        }

        if (StringUtils.isBlank(oidcProperties.getClientId())) {
            return null;
        }
        return logoutUrl + "&client_id="
                + URLEncoder.encode(oidcProperties.getClientId(), StandardCharsets.UTF_8);
    }

    /**
     * 登出后回跳的前端地址，取统一认证回调地址的同源根路径
     */
    private String getPostLogoutRedirectUri() {
        String callbackUri = oidcProperties.getFrontendCallbackUri();
        if (StringUtils.isBlank(callbackUri)) {
            return null;
        }

        URI callback = URI.create(callbackUri);
        if (callback.getScheme() == null || callback.getAuthority() == null) {
            return null;
        }

        return callback.getScheme() + "://" + callback.getAuthority() + "/";
    }
}
