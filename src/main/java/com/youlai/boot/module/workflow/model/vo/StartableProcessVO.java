package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 可发起流程VO（发起页流程选择列表）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "可发起流程对象")
@Getter
@Setter
public class StartableProcessVO {

    @Schema(description = "流程定义ID")
    private String id;

    @Schema(description = "流程定义标识")
    private String key;

    @Schema(description = "流程定义名称")
    private String name;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "绑定表单标识（开始节点 formKey，未绑定为空；空则直接发起，无表单填写）")
    private String formKey;

}
