package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户配置属性
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "youlai.tenant")
public class TenantProperties {

    /**
     * 是否启用多租户功能
     * 默认：false（不启用）
     */
    private Boolean enabled = false;

    /**
     * 租户字段名
     * 默认：tenant_id
     */
    private String column = "tenant_id";

    /**
     * 默认租户ID（用于兼容旧数据，tenant_id 为 NULL 时使用）
     * 默认：1
     */
    private Long defaultTenantId = 1L;

    /**
     * 忽略多租户过滤的表名列表
     * 系统表、租户表等不需要租户隔离的表
     */
    private List<String> ignoreTables = new ArrayList<>();

    /**
     * 请求头中的租户ID字段名
     * 默认：tenant-id
     */
    private String headerName = "tenant-id";

}

