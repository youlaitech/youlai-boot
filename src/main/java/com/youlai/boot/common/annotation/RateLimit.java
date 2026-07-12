package com.youlai.boot.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流
 * <p>标注在 Controller 方法上，基于 Redis 计数窗口实现</p>
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 窗口内允许的最大请求数。
     * <p>
     * {@code <=0} 时使用全局默认值 {@code rate-limit.default-limit}。
     */
    int limit() default 0;

    /**
     * 滑动窗口大小。
     * <p>
     * 单位由 {@link #timeUnit()} 决定；{@code <=0} 时使用全局默认值。
     */
    int window() default 0;

    /**
     * 窗口单位，默认秒。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流 Key 的分组标签，用于区分不同接口。
     * <p>
     * 默认 {@code api}。
     */
    String prefix() default "api";
}
