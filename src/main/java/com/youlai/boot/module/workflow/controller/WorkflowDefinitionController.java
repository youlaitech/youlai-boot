package com.youlai.boot.module.workflow.controller;

import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.workflow.model.form.DefinitionStateForm;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;
import com.youlai.boot.module.workflow.model.vo.ProcessStageVO;
import com.youlai.boot.module.workflow.model.vo.StartableProcessVO;
import com.youlai.boot.module.workflow.service.WorkflowDefinitionService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义控制层（发布产物：发布后供发起、审批与流程图查看）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Tag(name = "13.工作流定义接口")
@ConditionalOnWorkflowEnabled
@RestController
@RequestMapping("/api/v1/workflow/definitions")
@RequiredArgsConstructor
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    @Operation(summary = "可发起流程列表（发起页下拉数据源）")
    @GetMapping("/startable")
    public Result<List<StartableProcessVO>> listStartableProcesses() {
        return Result.success(workflowDefinitionService.listStartableProcesses());
    }

    @Operation(summary = "流程审批阶段预览（发起页/审批弹窗展示流程走向与各环节办理人）")
    @GetMapping("/{definitionId}/stages")
    public Result<List<ProcessStageVO>> listProcessStages(
            @Parameter(description = "流程定义ID") @PathVariable String definitionId
    ) {
        return Result.success(workflowDefinitionService.listProcessStages(definitionId));
    }

    @Operation(summary = "获取流程定义 BPMN XML（流程图查看）")
    @GetMapping("/{definitionId}/xml")
    @PreAuthorize("@ss.hasPerm('workflow:model:list')")
    public Result<ModelXmlVO> getDefinitionXml(
            @Parameter(description = "流程定义ID") @PathVariable String definitionId
    ) {
        return Result.success(workflowDefinitionService.getDefinitionXml(definitionId));
    }

    @Operation(summary = "启用/停用流程（停用后不可发起新流程，运行中的实例不受影响）")
    @PutMapping("/{definitionId}/state")
    @PreAuthorize("@ss.hasPerm('workflow:definition:update')")
    public Result<Void> updateDefinitionState(
            @Parameter(description = "流程定义ID") @PathVariable String definitionId,
            @Valid @RequestBody DefinitionStateForm formData
    ) {
        workflowDefinitionService.updateDefinitionState(definitionId, formData);
        return Result.success();
    }

}
