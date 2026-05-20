package com.youlai.boot.framework.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 配置
 *
 * @author Ray.Hao
 * @since 2023/5/15
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate
     * <p>
     * 修改 Redis 序列化方式，默认 JdkSerializationRedisSerializer
     *
     * @param redisConnectionFactory {@link RedisConnectionFactory}
     * @return {@link RedisTemplate}
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Key 使用 String 序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());

        // Value 使用自定义 JSON 序列化（不写入类型信息，避免 HashSet 等集合被序列化成带 @class 的结构）
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        JacksonJsonRedisSerializer<Object> jsonSerializer = new JacksonJsonRedisSerializer<>(jsonMapper, Object.class);

        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
