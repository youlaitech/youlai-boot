package com.youlai.boot.common.base;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 *
 * <p>实体类的基类，包含了实体类的公共属性，如创建时间、更新时间、逻辑删除标识等</p>
 * <p>多租户模式下，会自动添加 tenant_id 字段（通过 MyMetaObjectHandler 自动填充）</p>
 *
 * @author Ray
 * @since 2024/6/23
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（多租户模式）
     * <p>
     * 注意：此字段仅在启用多租户时生效
     * 通过 MyMetaObjectHandler 自动填充，无需手动设置
     * 如果不需要多租户，可以通过配置 youlai.tenant.enabled=false 禁用
     * </p>
     * <p>
     * 重要说明：
     * 1. 默认使用 exist = false 标记字段不存在于数据库，避免单租户模式下报错
     * 2. 在启用多租户时，需要确保数据库表中有 tenant_id 字段
     * 3. 多租户的数据隔离主要通过 TenantLineHandler 自动添加 WHERE 条件实现
     * 4. 如果需要在 INSERT 时写入 tenant_id，请将 exist 改为 true 或移除 exist 属性
     * 5. 或者执行 add_tenant_column.sql 脚本为表添加 tenant_id 字段
     * </p>
     */
    @TableField(value = "tenant_id", exist = false)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private Long tenantId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
