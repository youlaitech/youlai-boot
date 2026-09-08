package com.youlai.boot.module.form.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 表单渲染VO（渲染端按 formKey 加载的已发布规则）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单渲染对象")
@Getter
@Setter
public class FormRenderVO {

    @Schema(description = "表单唯一标识")
    private String formKey;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单版本号")
    private Integer version;

    @Schema(description = "表单规则（form-create rule 数组 JSON 字符串）")
    private String formJson;

    @Schema(description = "表单全局配置 JSON 字符串")
    private String optionsJson;

}
