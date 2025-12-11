package com.youlai.boot.config;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.youlai.boot.config.property.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 多租户动态字段配置
 * <p>
 * 在多租户模式启用时，动态修改 BaseEntity 中 tenant_id 字段的 exist 属性为 true
 * 这样可以实现：
 * - 单租户模式：tenant_id exist=false，不映射该字段，兼容没有该字段的表
 * - 多租户模式：tenant_id exist=true，自动填充租户ID到INSERT/UPDATE语句
 * </p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "youlai.tenant", name = "enabled", havingValue = "true")
public class TenantDynamicFieldConfig implements InitializingBean {

    private final TenantProperties tenantProperties;

    @Override
    public void afterPropertiesSet() {
        log.info("多租户模式已启用，开始动态配置 tenant_id 字段映射...");
        
        int modifiedCount = 0;
        List<TableInfo> tableInfos = TableInfoHelper.getTableInfos();
        
        for (TableInfo tableInfo : tableInfos) {
            // 检查是否是忽略的表
            String tableName = tableInfo.getTableName();
            if (tenantProperties.getIgnoreTables().contains(tableName)) {
                log.debug("表 {} 在忽略列表中，跳过 tenant_id 字段配置", tableName);
                continue;
            }
            
            // 查找 tenant_id 字段
            TableFieldInfo tenantField = tableInfo.getFieldList().stream()
                    .filter(field -> tenantProperties.getColumn().equals(field.getColumn()))
                    .findFirst()
                    .orElse(null);
            
            if (tenantField != null) {
                try {
                    // 通过反射修改 exist 属性为 true
                    Field existField = TableFieldInfo.class.getDeclaredField("exist");
                    existField.setAccessible(true);
                    existField.set(tenantField, true);
                    
                    modifiedCount++;
                    log.debug("已为表 {} 启用 tenant_id 字段映射", tableName);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.warn("修改表 {} 的 tenant_id 字段配置失败: {}", tableName, e.getMessage());
                }
            } else {
                log.warn("表 {} 未找到 tenant_id 字段，请检查实体类是否继承 BaseEntity", tableName);
            }
        }
        
        log.info("多租户字段配置完成，共修改 {} 张表", modifiedCount);
    }
}
