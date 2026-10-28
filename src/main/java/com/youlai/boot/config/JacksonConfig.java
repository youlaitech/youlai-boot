package com.youlai.boot.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Jackson 全局序列化配置
 *
 * @author Ray.Hao
 * @since 2026/1/12
 */
@Configuration
public class JacksonConfig {

    // 默认日期时间格式，Date 与 LocalDateTime 共用
    private static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 全局 JsonMapper
     */
    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_PATTERN))
                .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
                // LocalDateTime 没有全局格式开关，按类型覆盖默认格式；字段上的 @JsonFormat 仍优先
                .withConfigOverride(LocalDateTime.class,
                        override -> override.setFormat(JsonFormat.Value.forPattern(DEFAULT_DATE_TIME_PATTERN)))
                .addModule(new SimpleModule()
                        .addSerializer(Long.class, ToStringSerializer.instance)
                        .addSerializer(BigInteger.class, ToStringSerializer.instance)
                )
                .build();
    }

}
