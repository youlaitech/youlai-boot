package com.youlai.boot.config;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.youlai.boot.auth.password.config.CaptchaConfig;
import com.youlai.boot.auth.password.config.CaptchaProperties;
import com.youlai.boot.auth.password.service.CaptchaService;
import com.youlai.boot.framework.security.config.SecurityProperties;
import com.youlai.boot.framework.web.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证精简环境配置后，公共设置和代码默认值仍能支撑现有行为；不连接外部服务。
 */
class ConfigurationDefaultsTest {

    @Test
    @SuppressWarnings("unchecked")
    void captchaWorksWithoutAnyExplicitConfiguration() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        new ApplicationContextRunner()
                .withUserConfiguration(CaptchaDefaultsConfiguration.class, CaptchaConfig.class)
                .withBean(RedisTemplate.class, () -> redis)
                .withBean(CaptchaService.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var generator = context.getBean(CodeGenerator.class);
                    assertThat(generator).isInstanceOf(MathGenerator.class);
                    assertThat(generator.verify("1+2=", "3")).isTrue();

                    var captcha = context.getBean(CaptchaService.class).generate();
                    assertThat(captcha.getCaptchaId()).isNotBlank();
                    assertThat(captcha.getCaptchaBase64()).startsWith("data:image/png;base64,");
                    var imageBytes = Base64.getDecoder().decode(
                            captcha.getCaptchaBase64().substring(captcha.getCaptchaBase64().indexOf(',') + 1));
                    var image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    assertThat(image).isNotNull();
                    assertThat(image.getWidth()).isEqualTo(120);
                    assertThat(image.getHeight()).isEqualTo(40);
                    verify(values).set(anyString(), anyString(), eq(120L), eq(TimeUnit.SECONDS));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", "configuration-defaults-test"})
    void profilesInheritOperationalDefaults(String profile) {
        contextRunner(profile).run(context -> {
            assertThat(context).hasNotFailed();
            var binder = Binder.get(context.getEnvironment());

            var rateLimit = binder.bindOrCreate("rate-limit", RateLimitProperties.class);
            assertThat(rateLimit.getDefaultLimit()).isEqualTo(switch (profile) {
                case "dev" -> 60;
                case "prod" -> 10;
                default -> 5;
            });
            assertThat(rateLimit.getDefaultWindow()).isEqualTo(Duration.ofSeconds(60));
            assertThat(rateLimit.getIp().isEnabled()).isTrue();
            assertThat(rateLimit.getIp().getLimit()).isEqualTo(1000);
            assertThat(rateLimit.getIp().getWindow()).isEqualTo(Duration.ofSeconds(60));

            var security = binder.bindOrCreate("security", SecurityProperties.class);
            assertThat(security.getToken().getAccessTokenTimeToLive()).isEqualTo(Duration.ofHours(2));
            assertThat(security.getToken().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofDays(7));
            assertThat(security.getToken().isAllowMultiLogin()).isTrue();
            assertThat(security.getPermitAllUrls())
                    .contains("/api/v1/auth/login", "/api/v1/auth/captcha", "/swagger-ui.html")
                    .allSatisfy(url -> assertThat(url).doesNotContain("${"));

            var pool = binder.bindOrCreate("spring.data.redis", DataRedisProperties.class).getLettuce().getPool();
            assertThat(pool.getMaxActive()).isEqualTo(8);
            assertThat(pool.getMaxIdle()).isEqualTo(8);
            assertThat(pool.getMinIdle()).isZero();
            assertThat(pool.getMaxWait()).isEqualTo(Duration.ofMillis(-1));

            var multipart = binder.bindOrCreate("spring.servlet.multipart", MultipartProperties.class);
            assertThat(multipart.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(50));
            assertThat(multipart.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(50));

            var cache = binder.bindOrCreate("spring.cache", CacheProperties.class);
            assertThat(cache.getType()).isEqualTo(CacheType.NONE);
            assertThat(cache.getRedis().getTimeToLive()).isEqualTo(Duration.ofHours(1));
            assertThat(cache.getRedis().isCacheNullValues()).isTrue();
            var localCache = Caffeine.from(cache.getCaffeine().getSpec()).build();
            assertThat(localCache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(1000);
            assertThat(localCache.policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                    .isEqualTo(Duration.ofMinutes(10));
        });
    }

    @Test
    void multipartLimitsCanBeOverriddenIndependently() {
        contextRunner("dev")
                .withPropertyValues("spring.servlet.multipart.max-file-size=7MB",
                        "spring.servlet.multipart.max-request-size=8MB")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var multipart = Binder.get(context.getEnvironment())
                            .bindOrCreate("spring.servlet.multipart", MultipartProperties.class);
                    assertThat(multipart.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(7));
                    assertThat(multipart.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(8));
                });
    }

    private ApplicationContextRunner contextRunner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CaptchaProperties.class)
    static class CaptchaDefaultsConfiguration {
    }
}
