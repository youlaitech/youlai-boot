package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 流程模型 XML VO（设计器回显）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程模型XML对象")
@Getter
@Setter
public class ModelXmlVO {

    @Schema(description = "BPMN 2.0 XML（bpmn-js 导入渲染；新模型为空串）")
    private String xml;

    public ModelXmlVO(String xml) {
        this.xml = xml;
    }

}
