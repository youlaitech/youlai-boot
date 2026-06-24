package com.youlai.boot.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流
 * <p>标注在 Controller 方法上，基于 Redis 计数窗口实现</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int limit() default 0;

    int window() default 0;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String prefix() default "rate_limit:";
}
