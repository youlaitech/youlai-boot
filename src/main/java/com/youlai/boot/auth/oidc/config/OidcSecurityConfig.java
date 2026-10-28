package com.youlai.boot.auth.oidc.config;

import com.youlai.boot.auth.oidc.handler.OidcLoginHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * OIDC 授权与票据兑换的安全配置
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcSecurityConfig {

    private final OidcLoginHandler oidcLoginHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 临时授权会话只用于 OIDC 流程，不参与业务接口认证
     */
    @Bean
    @Order(0)
    public SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/oauth2/authorization/**", "/login/oauth2/code/**",
                        "/api/v1/auth/oidc/tickets/exchange")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(login -> login
                        .successHandler(oidcLoginHandler)
                        .failureHandler(oidcLoginHandler)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(createAuthorizationRequestResolver())))
                // 票据兑换校验一次性票据和浏览器 Cookie
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/oidc/tickets/exchange"))
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(withDefaults());
        return http.build();
    }

    /**
     * 授权码流程附加 PKCE（S256），授权请求的 code_verifier 保存在临时会话中
     */
    private OAuth2AuthorizationRequestResolver createAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }
}
