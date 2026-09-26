package com.youlai.boot.codegen.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 填充生成配置入参
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Schema(description = "AI 填充生成配置入参")
@Data
public class AiFillConfigForm {

    @Schema(description = "补充需求描述，可为空", example = "状态字段用下拉框，创建时间不出现在表单")
    private String requirement;
}
