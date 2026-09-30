package com.youlai.boot.module.workflow.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

/**
 * 工作流启用条件
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnBooleanProperty(prefix = "workflow", name = "enabled")
public @interface ConditionalOnWorkflowEnabled {
}
