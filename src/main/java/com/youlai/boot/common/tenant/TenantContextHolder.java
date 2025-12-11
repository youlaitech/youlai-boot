package com.youlai.boot.common.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.extern.slf4j.Slf4j;

/**
 * 租户上下文工具类
 * <p>
 * 使用 TransmittableThreadLocal 存储当前线程的租户ID，确保线程安全
 * 支持异步任务、线程池、消息队列等场景的上下文传递
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Slf4j
public class TenantContextHolder {

    /**
     * 租户ID线程本地变量
     * 使用 TransmittableThreadLocal 支持父子线程和线程池场景的值传递
     */
    private static final TransmittableThreadLocal<Long> TENANT_ID_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 忽略租户标志（用于某些场景下临时跳过租户过滤）
     */
    private static final TransmittableThreadLocal<Boolean> IGNORE_TENANT_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId != null) {
            TENANT_ID_HOLDER.set(tenantId);
            log.debug("设置当前租户ID: {}", tenantId);
        }
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，如果未设置则返回 null
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 设置忽略租户标志
     *
     * @param ignore 是否忽略
     */
    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT_HOLDER.set(ignore);
        log.debug("设置忽略租户标志: {}", ignore);
    }

    /**
     * 是否忽略租户
     *
     * @return true-忽略，false-不忽略
     */
    public static boolean isIgnoreTenant() {
        Boolean ignore = IGNORE_TENANT_HOLDER.get();
        return ignore != null && ignore;
    }

    /**
     * 清除当前线程的租户上下文
     * <p>
     * 必须在请求结束时调用，避免线程池复用导致的数据泄露
     * </p>
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
        IGNORE_TENANT_HOLDER.remove();
        log.debug("清除租户上下文");
    }
}

