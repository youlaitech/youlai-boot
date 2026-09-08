package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 流程图数据VO（bpmn-js 渲染 + 节点高亮）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程图数据对象")
@Getter
@Setter
public class ProcessDiagramVO {

    @Schema(description = "BPMN 2.0 XML（bpmn-js 导入渲染）")
    private String bpmnXml;

    @Schema(description = "已办节点ID列表（高亮为走过路径）")
    private List<String> executedActivityIds;

    @Schema(description = "进行中节点ID列表（高亮为当前待办）")
    private List<String> activeActivityIds;

}
