package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * 系统配置对象
 *
 * @author Theo
 * @since 2024-07-29 11:17:26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置")
@TableName("sys_config")
public class Config extends BaseEntity {

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 描述、备注
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 逻辑删除标识(0-未删除 1-已删除)
     */
    private Integer isDeleted;

}
