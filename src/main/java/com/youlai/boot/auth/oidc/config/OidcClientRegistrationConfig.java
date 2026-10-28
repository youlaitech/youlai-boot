package com.youlai.boot.auth.oidc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

/**
 * 启用 OIDC 时注册客户端并读取发现文档
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcClientRegistrationConfig {

    // OAuth 入口与回调的注册标识；不作为持久化账号归属
    public static final String REGISTRATION_ID = "youlai";

    private final OidcProperties oidcProperties;

    /**
     * 根据 issuer 发现端点，使用授权码登录
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistrations
                .fromIssuerLocation(oidcProperties.getIssuer())
                .registrationId(REGISTRATION_ID)
                .clientName("统一认证")
                .clientId(oidcProperties.getClientId())
                .clientSecret(oidcProperties.getClientSecret())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE)
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
