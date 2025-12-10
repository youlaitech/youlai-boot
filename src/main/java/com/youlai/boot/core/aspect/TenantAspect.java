package com.youlai.boot.core.aspect;

import com.youlai.boot.common.annotation.IgnoreTenant;
import com.youlai.boot.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 多租户切面
 * <p>
 * 处理 @IgnoreTenant 注解，临时跳过租户过滤
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Aspect
@Component
@Order(1)
@Slf4j
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TenantAspect {

    /**
     * 环绕通知：处理 @IgnoreTenant 注解
     */
    @Around("@annotation(ignoreTenant) || @within(ignoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint, IgnoreTenant ignoreTenant) throws Throwable {
        try {
            // 设置忽略租户标志
            TenantContextHolder.setIgnoreTenant(true);
            log.debug("方法 {} 忽略多租户过滤", joinPoint.getSignature().getName());
            // 执行原方法
            return joinPoint.proceed();
        } finally {
            // 恢复租户过滤
            TenantContextHolder.setIgnoreTenant(false);
        }
    }
}

