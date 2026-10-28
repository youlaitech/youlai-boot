package com.youlai.boot.auth.oidc.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

/**
 * 统一认证启用条件
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnBooleanProperty(prefix = "security.oidc", name = "enabled")
public @interface ConditionalOnOidcEnabled {
}
