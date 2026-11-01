package com.youlai.boot.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.util.ArrayUtil;
import com.youlai.boot.auth.password.service.CaptchaService;
import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.security.handler.JsonAccessDeniedHandler;
import com.youlai.boot.framework.security.handler.JsonAuthenticationEntryPoint;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.framework.security.service.SecurityUserDetailsService;
import com.youlai.boot.framework.security.session.SessionService;
import com.youlai.boot.auth.password.filter.CaptchaValidationFilter;
import com.youlai.boot.auth.sms.provider.SmsAuthenticationProvider;
import com.youlai.boot.auth.wxma.provider.WxMaAuthenticationProvider;
import com.youlai.boot.auth.wxma.config.WxMaProperties;
import com.youlai.boot.system.service.UserIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;

/**
 * Spring Security 配置类
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
    private final SessionService sessionService;
    private final SecurityUserDetailsService userDetailsService;
    private final CaptchaService captchaService;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requestMatcherRegistry -> {
                            // 异步响应的后续分发复用首次请求认证结果
                            requestMatcherRegistry.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                            String[] permitAllUrls = securityProperties.getPermitAllUrls();
                            if (ArrayUtil.isNotEmpty(permitAllUrls)) {
                                requestMatcherRegistry.requestMatchers(permitAllUrls).permitAll();
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
                .addFilterBefore(new CaptchaValidationFilter(captchaService), UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(resource -> resource
                        .authenticationManagerResolver(request -> authentication -> {
                            var bearer = (BearerTokenAuthenticationToken) authentication;
                            return sessionService.authenticate(bearer.getToken()).toAuthentication();
                        })
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                                var failureHandler = new AuthenticationEntryPointFailureHandler(new JsonAuthenticationEntryPoint());
                                // 存储故障返回 JSON 503，不交给容器生成错误页
                                failureHandler.setRethrowAuthenticationServiceException(false);
                                filter.setAuthenticationFailureHandler(failureHandler);
                                return filter;
                            }
                        })
                        .accessDeniedHandler(new JsonAccessDeniedHandler()));

        return http.build();
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
            WxMaProperties wxMaProperties,
            UserAuthenticationPort userAuthenticationPort,
            UserIdentityService userIdentityService
    ) {
        return new WxMaAuthenticationProvider(wxMaService, wxMaProperties, userAuthenticationPort,
                userIdentityService, redisTemplate);
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
