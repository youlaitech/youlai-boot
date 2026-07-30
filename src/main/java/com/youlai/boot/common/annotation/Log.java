package com.youlai.boot.common.annotation;

import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志注解
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Log {

    /**
     * 模块
     *
     * @return 模块
     */
    LogModuleEnum module();

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    ActionTypeEnum value();

    /**
     * 操作标题（可选，默认使用枚举描述）
     *
     * @return 标题
     */
    String title() default "";

    /**
     * 自定义日志内容（可选，用于记录操作细节）
     *
     * @return 日志内容
     */
    String content() default "";

}
