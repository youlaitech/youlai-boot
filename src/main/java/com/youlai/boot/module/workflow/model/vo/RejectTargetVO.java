package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 驳回目标节点VO（驳回下拉选择）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "驳回目标节点对象")
@Getter
@Setter
public class RejectTargetVO {

    @Schema(description = "节点ID")
    private String activityId;

    @Schema(description = "节点名称")
    private String activityName;

    public RejectTargetVO(String activityId, String activityName) {
        this.activityId = activityId;
        this.activityName = activityName;
    }

}
