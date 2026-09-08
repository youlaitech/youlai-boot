package com.youlai.boot.framework.web.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * <p>标注在 Controller 方法上，基于 Redis 滑动窗口实现</p>
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
     * 滑动窗口大小（秒）。
     * <p>
     * {@code <=0} 时使用全局默认值 {@code rate-limit.default-window}。
     */
    int window() default 0;
}
