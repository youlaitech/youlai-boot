package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 租户VO
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@Schema(description = "租户信息")
public class TenantVO implements Serializable {

    @Schema(description = "租户ID")
    private Long id;

    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "租户编码")
    private String code;

    @Schema(description = "租户状态(1-正常 0-禁用)")
    private Integer status;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "租户域名")
    private String domain;

    @Schema(description = "租户Logo")
    private String logo;

    @Schema(description = "是否默认租户")
    private Boolean isDefault;
}

