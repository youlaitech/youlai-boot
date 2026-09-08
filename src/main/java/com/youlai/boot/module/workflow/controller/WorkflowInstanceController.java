package com.youlai.boot.module.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.workflow.model.form.StartProcessForm;
import com.youlai.boot.module.workflow.model.query.WorkflowInstancePageQuery;
import com.youlai.boot.module.workflow.model.vo.InstanceDetailVO;
import com.youlai.boot.module.workflow.model.vo.InstancePageVO;
import com.youlai.boot.module.workflow.model.vo.ProcessDiagramVO;
import com.youlai.boot.module.workflow.service.WorkflowInstanceService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 流程实例控制层（发起、我的流程、详情、流程图、撤销/终止）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Tag(name = "13.工作流实例接口")
@ConditionalOnWorkflowEnabled
@RestController
@RequestMapping("/api/v1/workflow/instances")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    @Operation(summary = "发起流程")
    @PostMapping
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.INSERT, title = "发起流程")
    public Result<String> startProcess(@Valid @RequestBody StartProcessForm formData) {
        return Result.success(workflowInstanceService.startProcess(formData));
    }

    @Operation(summary = "我的流程分页列表")
    @GetMapping
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.LIST, title = "我的流程")
    public PageResult<InstancePageVO> getInstancePage(@Valid WorkflowInstancePageQuery queryParams) {
        Page<InstancePageVO> result = workflowInstanceService.getInstancePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "获取流程图数据（节点高亮）")
    @GetMapping("/{instanceId}/diagram")
    public Result<ProcessDiagramVO> getProcessDiagram(
            @Parameter(description = "流程实例ID") @PathVariable String instanceId
    ) {
        return Result.success(workflowInstanceService.getProcessDiagram(instanceId));
    }

    @Operation(summary = "获取流程实例详情")
    @GetMapping("/{instanceId}")
    public Result<InstanceDetailVO> getInstanceDetail(
            @Parameter(description = "流程实例ID") @PathVariable String instanceId
    ) {
        return Result.success(workflowInstanceService.getInstanceDetail(instanceId));
    }

    @Operation(summary = "撤销流程（仅发起人）")
    @PutMapping("/{instanceId}/cancel")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.DELETE, title = "撤销流程")
    public Result<Void> cancelProcessInstance(
            @Parameter(description = "流程实例ID") @PathVariable String instanceId
    ) {
        workflowInstanceService.cancelProcessInstance(instanceId);
        return Result.success();
    }

    @Operation(summary = "终止流程（管理员）")
    @PutMapping("/{instanceId}/terminate")
    @PreAuthorize("@ss.hasPerm('workflow:instance:terminate')")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.DELETE, title = "终止流程")
    public Result<Void> terminateProcessInstance(
            @Parameter(description = "流程实例ID") @PathVariable String instanceId,
            @Parameter(description = "终止原因") @RequestParam(required = false) String reason
    ) {
        workflowInstanceService.terminateProcessInstance(instanceId, reason);
        return Result.success();
    }

}
