package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程模型 XML 表单对象（设计器保存）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程模型XML表单对象")
@Data
public class ModelXmlForm {

    @Schema(description = "BPMN 2.0 XML（bpmn-js 产出）")
    @NotBlank(message = "流程模型内容不能为空")
    private String xml;

}
