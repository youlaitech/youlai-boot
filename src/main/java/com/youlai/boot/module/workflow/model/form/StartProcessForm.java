package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 发起流程表单对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "发起流程表单对象")
@Data
public class StartProcessForm {

    @Schema(description = "流程定义ID")
    @NotBlank(message = "流程定义ID不能为空")
    private String processDefinitionId;

    @Schema(description = "表单数据（field -> value 映射；流程绑定 formKey 时必填）")
    private Map<String, Object> formData;

    @Schema(description = "流程变量（驱动网关条件；随实例提交）")
    private Map<String, Object> variables;

    @Schema(description = "流程实例名称（未传时默认取流程定义名称）")
    private String name;

}
