package com.youlai.boot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 缓存的序列化与过期策略，缓存管理器由 Spring Boot 自动装配
 *
 * @author Ray.Hao
 * @since 2023/12/4
 */
@EnableConfigurationProperties(CacheProperties.class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis")
public class RedisCacheConfig {

    /**
     * 自定义 RedisCacheConfiguration
     *
     * @param cacheProperties {@link CacheProperties}
     * @return {@link RedisCacheConfiguration}
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        CacheProperties.Redis redisProperties = cacheProperties.getRedis();

        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        // 保留现有 cacheName: 前缀格式，并支持 spring.cache.redis.key-prefix
        String keyPrefix = redisProperties.getKeyPrefix();
        config = config.computePrefixWith(name -> (keyPrefix != null ? keyPrefix : "") + name + ":");
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

}
