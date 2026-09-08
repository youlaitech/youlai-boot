package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 流程模型VO（分页列表）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程模型对象")
@Getter
@Setter
public class ModelVO {

    @Schema(description = "模型ID")
    private String id;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "模型标识")
    private String key;

    @Schema(description = "模型描述")
    private String description;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "最新发布定义ID（未发布为 null）")
    private String definitionId;

    @Schema(description = "最新发布版本号（未发布为 null）")
    private Integer publishedVersion;

    @Schema(description = "发布状态：true=已停用，false=已启用（未发布为 null）")
    private Boolean suspended;

    @Schema(description = "发布时间（未发布为 null）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deploymentTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

}
