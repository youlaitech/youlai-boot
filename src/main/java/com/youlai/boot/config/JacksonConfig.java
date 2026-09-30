package com.youlai.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson 全局序列化配置
 *
 * @author Ray.Hao
 * @since 2026/1/12
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局 JsonMapper
     */
    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
                .addModule(new SimpleModule()
                        .addSerializer(Long.class, ToStringSerializer.instance)
                        .addSerializer(BigInteger.class, ToStringSerializer.instance)
                )
                .build();
    }

}
