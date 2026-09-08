package com.youlai.boot.framework.web.repeatsubmit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 *
 * <p>标注在写接口方法上，指定时间窗口内同一用户对同一请求只允许提交一次，默认 5 秒。</p>
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
     *
     * <p>默认 5 秒内不允许重复提交，占位到期后自动释放。</p>
     */
    int expire() default 5;

    /**
     * 请求标识 SpEL 表达式（可选）
     *
     * <p>非空时以表达式的值区分不同请求，如 {@code "#form.name"}；
     * 为空时基于方法参数自动生成请求指纹，自动排除 ServletRequest/ServletResponse、
     * MultipartFile、BindingResult 等不可稳定序列化的参数。</p>
     */
    String key() default "";
}
