package com.youlai.boot.plugin.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.youlai.boot.common.tenant.TenantContextHolder;
import com.youlai.boot.config.property.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * mybatis-plus 字段自动填充
 * <p>
 * 支持自动填充创建时间、更新时间和租户ID
 * </p>
 *
 * @author haoxr
 * @since 2022/10/14
 */
@Component
@RequiredArgsConstructor
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Autowired(required = false)
    private TenantProperties tenantProperties;

    /**
     * 新增填充创建时间、更新时间和租户ID
     *
     * @param metaObject 元数据
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // 如果启用了多租户，自动填充租户ID
        // 注意：由于 BaseEntity 中 tenantId 字段使用了 exist = false（避免单租户模式报错）
        // 在启用多租户时，需要通过反射动态修改字段的 exist 属性，或者直接设置值
        // 但 MyBatis-Plus 的字段映射是静态的，无法动态修改
        // 因此，我们使用 strictInsertFill，它会自动处理字段映射
        // 如果字段不存在（exist = false），strictInsertFill 会跳过，不会报错
        if (tenantProperties != null && Boolean.TRUE.equals(tenantProperties.getEnabled())) {
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                // 使用数据库字段名（tenant_id）进行填充
                // 注意：由于 exist = false，这个填充不会写入数据库
                // 但多租户的数据隔离是通过 TenantLineHandler 自动添加 WHERE 条件实现的
                // 所以这里只需要设置实体对象的属性值即可（用于业务逻辑）
                String propertyName = "tenantId";
                if (metaObject.hasGetter(propertyName)) {
                    // 直接设置值到实体对象，不依赖字段映射
                    metaObject.setValue(propertyName, tenantId);
                }
            }
        }
    }

    /**
     * 更新填充更新时间
     *
     * @param metaObject 元数据
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

}
