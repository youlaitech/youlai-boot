package com.youlai.boot.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Web 配置
 *
 * @author Ray.Hao
 * @since 2020/10/16
 */
@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Spring Boot 4.x 已经使用Jackson 3.x
     * Jackson 3.x已经自带JavaTimeModule，不再需要手工注册
     *
     * @param converterBuilder the builder to configure
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder converterBuilder) {
        JsonMapper.Builder builder = JsonMapper.builder();

        // 配置全局日期格式和时区
        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        builder.defaultTimeZone(TimeZone.getTimeZone("GMT+8"));

        // 处理 Long/BigInteger 的精度问题
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        builder.addModule(simpleModule);

        converterBuilder.addCustomConverter(new JacksonJsonHttpMessageConverter(builder));
    }

    /**
     * 配置校验器
     *
     * @param autowireCapableBeanFactory 用于注入 SpringConstraintValidatorFactory
     * @return Validator 实例
     */
    @Bean
    public Validator validator(final AutowireCapableBeanFactory autowireCapableBeanFactory) {
        try (ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(true) // failFast=true 时，遇到第一个校验失败则立即返回，false 表示校验所有参数
                .constraintValidatorFactory(new SpringConstraintValidatorFactory(autowireCapableBeanFactory))
                .buildValidatorFactory()) {

            // 使用 try-with-resources 确保 ValidatorFactory 被正确关闭
            return validatorFactory.getValidator();
        }
    }
}
