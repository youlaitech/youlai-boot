package com.youlai.boot.plugin.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MyBatis-Plus 多租户处理器
 * <p>
 * 实现 TenantLineHandler 接口，自动为 SQL 添加租户过滤条件
 * 仅在启用多租户时注册（通过 @ConditionalOnProperty 控制）
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

    private final TenantProperties tenantProperties;

    /**
     * 获取租户ID表达式
     * <p>
     * 从 TenantContextHolder 获取当前租户ID
     * 如果未设置或忽略租户，返回 NULL（不添加租户条件）
     * </p>
     *
     * @return 租户ID表达式
     */
    @Override
    public Expression getTenantId() {
        // 如果设置了忽略租户标志，返回 NULL（不添加租户条件）
        if (TenantContextHolder.isIgnoreTenant()) {
            return new NullValue();
        }

        // 获取当前租户ID
        Long tenantId = TenantContextHolder.getTenantId();

        // 如果未设置租户ID，使用默认租户ID
        if (tenantId == null) {
            tenantId = tenantProperties.getDefaultTenantId();
        }

        return new LongValue(tenantId);
    }

    /**
     * 获取租户字段名
     *
     * @return 租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getColumn();
    }

    /**
     * 判断表是否忽略多租户过滤
     * <p>
     * 系统表、租户表等不需要租户隔离的表应返回 true
     * </p>
     *
     * @param tableName 表名
     * @return true-忽略，false-不忽略
     */
    @Override
    public boolean ignoreTable(String tableName) {
        List<String> ignoreTables = tenantProperties.getIgnoreTables();
        if (ignoreTables == null || ignoreTables.isEmpty()) {
            return false;
        }

        // 忽略表名匹配（不区分大小写）
        return ignoreTables.stream()
                .anyMatch(ignoreTable -> ignoreTable.equalsIgnoreCase(tableName));
    }
}

