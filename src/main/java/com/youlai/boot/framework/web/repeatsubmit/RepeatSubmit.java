package com.youlai.boot.framework.web.repeatsubmit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 *
 * @author Ray.Hao
 * @since 2.3.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 防重占位过期时间（秒）
     */
    int expire() default 5;

    /**
     * 请求标识 SpEL 表达式（可选）
     */
    String key() default "";
}
