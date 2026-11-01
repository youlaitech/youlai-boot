package com.youlai.boot.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 启用缓存注解，由 Spring Boot 根据 spring.cache.type 选择唯一的缓存管理器
 * none 使用空操作管理器，不缓存结果，也不影响登录会话等直接使用 Redis 的功能
 */
@EnableCaching
@Configuration(proxyBeanMethods = false)
public class CacheConfig {
}
