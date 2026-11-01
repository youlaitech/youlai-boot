package com.youlai.boot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 验证真实缓存注解与 Spring Boot 管理器选择，不连接数据库或 Redis。
 */
class CacheConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
            .withUserConfiguration(CacheConfig.class, RedisCacheConfig.class)
            .withBean(CountingService.class);

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", "cache-config-test"})
    void applicationDefaultsDisableCaching(String profile) {
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class);
                    assertThat(context).doesNotHaveBean(RedisCacheConfig.class);
                    assertThat(context).doesNotHaveBean(RedisConnectionFactory.class);
                    assertThat(context.getEnvironment().getProperty("spring.cache.type")).isEqualTo("none");
                });
    }

    @Test
    void noneRunsEveryInvocationWithoutStoringResults() {
        runner.withPropertyValues("spring.cache.type=none").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class);
            var service = context.getBean(CountingService.class);
            assertThat(service.load("item")).isEqualTo(1);
            assertThat(service.load("item")).isEqualTo(2);
            service.evict("item");
            assertThat(service.load("item")).isEqualTo(3);
        });
    }

    @Test
    void caffeineCachesAndEvictsThroughAnnotations() {
        runner.withPropertyValues("spring.cache.type=caffeine",
                        "spring.cache.caffeine.spec=maximumSize=7,expireAfterWrite=1m")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
                    assertThat(context).doesNotHaveBean(RedisCacheConfig.class);
                    var service = context.getBean(CountingService.class);
                    assertThat(service.load("item")).isEqualTo(1);
                    assertThat(service.load("item")).isEqualTo(1);
                    service.evict("item");
                    assertThat(service.load("item")).isEqualTo(2);

                    var cache = (CaffeineCache) context.getBean(CacheManager.class).getCache("probe");
                    assertThat(cache).isNotNull();
                    assertThat(cache.getNativeCache().policy().eviction().orElseThrow().getMaximum()).isEqualTo(7);
                    assertThat(cache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                            .isEqualTo(Duration.ofMinutes(1));
                });
    }

    @Test
    void redisUsesOneManagerAndPreservesExistingStorageFormat() {
        var connectionFactory = mock(RedisConnectionFactory.class);
        runner.withBean(RedisConnectionFactory.class, () -> connectionFactory)
                .withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=3600000")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
                    var cache = (RedisCache) context.getBean(CacheManager.class).getCache("probe");
                    assertThat(cache).isNotNull();
                    var config = cache.getCacheConfiguration();
                    assertThat(config.getTtlFunction().getTimeToLive("key", "value")).isEqualTo(Duration.ofHours(1));
                    assertThat(config.getAllowCacheNullValues()).isTrue();
                    assertThat(config.usePrefix()).isTrue();
                    assertThat(config.getKeyPrefixFor("probe")).isEqualTo("probe:");
                    assertThat(StandardCharsets.UTF_8.decode(config.getKeySerializationPair().write("item")).toString())
                            .isEqualTo("item");
                    var encodedValue = config.getValueSerializationPair().write("cached-value");
                    assertThat(StandardCharsets.UTF_8.decode(encodedValue.duplicate()).toString())
                            .isEqualTo("\"cached-value\"");
                    assertThat(config.getValueSerializationPair().read(encodedValue)).isEqualTo("cached-value");
                    verify(connectionFactory, never()).getConnection();
                    verify(connectionFactory, never()).getClusterConnection();
                });
    }

    @Test
    void redisAppliesConfiguredPrefixTtlAndNullPolicy() {
        runner.withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=90s",
                        "spring.cache.redis.key-prefix=tenant:", "spring.cache.redis.cache-null-values=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    var cache = (RedisCache) context.getBean(CacheManager.class).getCache("probe");
                    assertThat(cache).isNotNull();
                    var config = cache.getCacheConfiguration();
                    assertThat(config.getTtlFunction().getTimeToLive("key", "value")).isEqualTo(Duration.ofSeconds(90));
                    assertThat(config.getAllowCacheNullValues()).isFalse();
                    assertThat(config.getKeyPrefixFor("probe")).isEqualTo("tenant:probe:");
                });
    }

    @Test
    void redisPrefixCanBeDisabled() {
        runner.withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withPropertyValues("spring.cache.type=redis", "spring.cache.redis.key-prefix=tenant:",
                        "spring.cache.redis.use-key-prefix=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    var cache = (RedisCache) context.getBean(CacheManager.class).getCache("probe");
                    assertThat(cache).isNotNull();
                    assertThat(cache.getCacheConfiguration().usePrefix()).isFalse();
                });
    }

    @ParameterizedTest
    @CsvSource({"none,true", "caffeine,false"})
    void legacyEnabledFlagDoesNotOverrideExplicitType(String type, boolean legacyEnabled) {
        runner.withPropertyValues("spring.cache.type=" + type, "spring.cache.enabled=" + legacyEnabled)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class))
                            .isInstanceOf("none".equals(type) ? NoOpCacheManager.class : CaffeineCacheManager.class);
                });
    }

    @Test
    void redisWithoutConnectionFactoryFailsInsteadOfSelectingAnotherProvider() {
        runner.withPropertyValues("spring.cache.type=redis").run(context -> assertThat(context).hasFailed());
    }

    static class CountingService {

        private final AtomicInteger calls = new AtomicInteger();

        @Cacheable(cacheNames = "probe", key = "#p0")
        public int load(String key) {
            return calls.incrementAndGet();
        }

        @CacheEvict(cacheNames = "probe", key = "#p0")
        public void evict(String key) {
        }
    }
}
