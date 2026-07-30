package com.youlai.boot.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.util.ArrayUtil;
import com.youlai.boot.auth.service.CaptchaService;
import com.youlai.boot.config.property.SecurityProperties;
import com.youlai.boot.framework.security.filter.TokenAuthenticationFilter;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.framework.security.service.SecurityUserDetailsService;
import com.youlai.boot.framework.security.token.TokenManager;
import com.youlai.boot.auth.security.filter.CaptchaValidationFilter;
import com.youlai.boot.auth.security.handler.JsonAccessDeniedHandler;
import com.youlai.boot.auth.security.handler.JsonAuthenticationEntryPoint;
import com.youlai.boot.auth.security.provider.SmsAuthenticationProvider;
import com.youlai.boot.auth.security.provider.WxMaAuthenticationProvider;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import jakarta.servlet.DispatcherType;

/**
 * Spring Security 配置类。
 * <p>
 * 归使用方（auth 模块），安全规则（放行路径、CORS、Provider 装配、响应格式）
 * 因项目而异，不应由框架层强制装配。
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final SecurityUserDetailsService userDetailsService;
    private final CaptchaService captchaService;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requestMatcherRegistry -> {
                            // ASYNC 分发（SSE 超时/完成后的异步 dispatch）无 Authorization 头，直接放行
                            requestMatcherRegistry.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                            String[] ignoreUrls = securityProperties.getIgnoreUrls();
                            if (ArrayUtil.isNotEmpty(ignoreUrls)) {
                                requestMatcherRegistry.requestMatchers(ignoreUrls).permitAll();
                            }
                            requestMatcherRegistry.anyRequest().authenticated();
                        }
                )
                .exceptionHandling(configurer ->
                        configurer
                                .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                                .accessDeniedHandler(new JsonAccessDeniedHandler())
                )
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                // 验证码校验（使用方过滤器，直接写 JSON 响应）
                .addFilterBefore(new CaptchaValidationFilter(captchaService), UsernamePasswordAuthenticationFilter.class)
                // Token 认证（Starter 过滤器，抛 AuthenticationException 交给 ExceptionTranslationFilter 处理）
                .addFilterBefore(new TokenAuthenticationFilter(tokenManager), AuthorizationFilter.class)
                .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            String[] unsecuredUrls = securityProperties.getUnsecuredUrls();
            if (ArrayUtil.isNotEmpty(unsecuredUrls)) {
                web.ignoring().requestMatchers(unsecuredUrls);
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SmsAuthenticationProvider smsAuthenticationProvider(UserAuthenticationPort userAuthPort) {
        return new SmsAuthenticationProvider(userAuthPort, redisTemplate);
    }

    @Bean
    public WxMaAuthenticationProvider wechatMiniAuthenticationProvider(
            WxMaService wxMaService,
            UserAuthenticationPort userAuthenticationPort,
            UserSocialService userSocialService
    ) {
        return new WxMaAuthenticationProvider(wxMaService, userAuthenticationPort, userSocialService);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider,
            SmsAuthenticationProvider smsAuthenticationProvider,
            WxMaAuthenticationProvider wxMaAuthenticationProvider
    ) {
        return new ProviderManager(
                daoAuthenticationProvider,
                smsAuthenticationProvider,
                wxMaAuthenticationProvider
        );
    }
}
