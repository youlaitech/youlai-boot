package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回任务表单对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "驳回任务表单对象")
@Data
public class RejectTaskForm {

    @Schema(description = "目标节点ID（驳回到的历史节点）")
    @NotBlank(message = "目标节点不能为空")
    private String targetActivityId;

    @Schema(description = "驳回意见（写入引擎评论，随任务历史归档）")
    @Size(max = 500, message = "驳回意见长度不能超过500个字符")
    private String comment;

}
