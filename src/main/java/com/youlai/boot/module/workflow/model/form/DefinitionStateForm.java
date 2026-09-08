package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流程定义状态表单对象（挂起/激活）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程定义状态表单对象")
@Data
public class DefinitionStateForm {

    @Schema(description = "是否挂起（true 挂起；false 激活）")
    @NotNull(message = "挂起状态不能为空")
    private Boolean suspend;

}
