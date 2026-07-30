package com.youlai.boot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * caffeine缓存配置
 *
 * @author Theo
 * @since 2025-01-22 17:40:23
 */
@Slf4j
@Configuration
public class CaffeineConfig {

    @Value("${spring.cache.caffeine.spec}")
    private String caffeineSpec;

    /**
     * 缓存管理器
     *
     * @return CacheManager 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeineBuilder = Caffeine.from(caffeineSpec);
        caffeineCacheManager.setCaffeine(caffeineBuilder);
        return caffeineCacheManager;
    }
}

