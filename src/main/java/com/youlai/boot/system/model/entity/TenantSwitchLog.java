package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户切换审计日志实体
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@TableName("sys_tenant_switch_log")
public class TenantSwitchLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 原租户ID
     */
    private Long fromTenantId;

    /**
     * 原租户名称
     */
    private String fromTenantName;

    /**
     * 目标租户ID
     */
    private Long toTenantId;

    /**
     * 目标租户名称
     */
    private String toTenantName;

    /**
     * 切换时间
     */
    private LocalDateTime switchTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 浏览器信息
     */
    private String userAgent;

    /**
     * 切换状态（1-成功 0-失败）
     */
    private Integer status;

    /**
     * 失败原因
     */
    private String failReason;
}
