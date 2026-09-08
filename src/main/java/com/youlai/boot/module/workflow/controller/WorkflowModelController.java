package com.youlai.boot.module.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.workflow.model.form.ModelForm;
import com.youlai.boot.module.workflow.model.form.ModelXmlForm;
import com.youlai.boot.module.workflow.model.query.WorkflowModelPageQuery;
import com.youlai.boot.module.workflow.model.vo.ModelVO;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;
import com.youlai.boot.module.workflow.service.WorkflowDemoService;
import com.youlai.boot.module.workflow.service.WorkflowModelService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 流程模型控制层（设计器侧：模型 CRUD、XML、部署）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Tag(name = "13.工作流模型接口")
@ConditionalOnWorkflowEnabled
@RestController
@RequestMapping("/api/v1/workflow/models")
@RequiredArgsConstructor
public class WorkflowModelController {

    private final WorkflowModelService workflowModelService;
    private final WorkflowDemoService workflowDemoService;

    @Operation(summary = "流程模型分页列表")
    @GetMapping
    @PreAuthorize("@ss.hasPerm('workflow:model:list')")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.LIST)
    public PageResult<ModelVO> getModelPage(@Valid WorkflowModelPageQuery queryParams) {
        Page<ModelVO> result = workflowModelService.getModelPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增流程模型")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('workflow:model:create')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.INSERT)
    public Result<Void> saveModel(@Valid @RequestBody ModelForm formData) {
        boolean result = workflowModelService.saveModel(formData);
        return Result.judge(result);
    }

    @Operation(summary = "修改流程模型")
    @PutMapping("/{modelId}")
    @PreAuthorize("@ss.hasPerm('workflow:model:update')")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.UPDATE)
    public Result<Void> updateModel(
            @Parameter(description = "模型ID") @PathVariable String modelId,
            @Valid @RequestBody ModelForm formData
    ) {
        boolean result = workflowModelService.updateModel(modelId, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除流程模型")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('workflow:model:delete')")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.DELETE)
    public Result<Void> removeModels(
            @Parameter(description = "模型ID，多个以英文逗号(,)拼接") @PathVariable String ids
    ) {
        workflowModelService.removeModels(Arrays.asList(ids.split(",")));
        return Result.success();
    }

    @Operation(summary = "获取模型 BPMN XML（设计器回显）")
    @GetMapping("/{modelId}/xml")
    @PreAuthorize("@ss.hasPerm('workflow:model:list')")
    public Result<ModelXmlVO> getModelXml(
            @Parameter(description = "模型ID") @PathVariable String modelId
    ) {
        return Result.success(workflowModelService.getModelXml(modelId));
    }

    @Operation(summary = "保存模型 BPMN XML（设计器保存）")
    @PutMapping("/{modelId}/xml")
    @PreAuthorize("@ss.hasPerm('workflow:model:update')")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.UPDATE)
    public Result<Void> saveModelXml(
            @Parameter(description = "模型ID") @PathVariable String modelId,
            @Valid @RequestBody ModelXmlForm formData
    ) {
        boolean result = workflowModelService.saveModelXml(modelId, formData);
        return Result.judge(result);
    }

    @Operation(summary = "部署流程模型")
    @PostMapping("/{modelId}/deploy")
    @PreAuthorize("@ss.hasPerm('workflow:model:deploy')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.INSERT, title = "部署流程")
    public Result<Void> deployModel(
            @Parameter(description = "模型ID") @PathVariable String modelId
    ) {
        workflowModelService.deployModel(modelId);
        return Result.success();
    }

    @Operation(summary = "重置工作流数据", description = "清空所有流程（含用户自建）的模型、定义、实例与历史，清理关联表单数据后重建初始演示流程")
    @PostMapping("/demo/reset")
    @PreAuthorize("@ss.hasPerm('workflow:model:reset')")
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.DELETE, title = "重置工作流数据")
    public Result<Void> resetDemoProcesses() {
        workflowDemoService.reset();
        return Result.success();
    }

}
