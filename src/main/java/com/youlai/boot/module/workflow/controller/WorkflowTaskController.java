package com.youlai.boot.module.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.framework.web.repeatsubmit.RepeatSubmit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.module.workflow.model.form.CompleteTaskForm;
import com.youlai.boot.module.workflow.model.form.RejectTaskForm;
import com.youlai.boot.module.workflow.model.query.WorkflowTaskPageQuery;
import com.youlai.boot.module.workflow.model.vo.DoneTaskVO;
import com.youlai.boot.module.workflow.model.vo.RejectTargetVO;
import com.youlai.boot.module.workflow.model.vo.TaskDetailVO;
import com.youlai.boot.module.workflow.model.vo.TodoTaskVO;
import com.youlai.boot.module.workflow.service.WorkflowTaskService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程任务控制层（待办/已办、审批通过/驳回）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Tag(name = "13.工作流任务接口")
@ConditionalOnWorkflowEnabled
@RestController
@RequestMapping("/api/v1/workflow/tasks")
@RequiredArgsConstructor
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;

    @Operation(summary = "待办任务分页列表")
    @GetMapping("/todo")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.LIST, title = "待办任务")
    public PageResult<TodoTaskVO> getTodoTaskPage(@Valid WorkflowTaskPageQuery queryParams) {
        Page<TodoTaskVO> result = workflowTaskService.getTodoTaskPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "已办任务分页列表")
    @GetMapping("/done")
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.LIST, title = "已办任务")
    public PageResult<DoneTaskVO> getDoneTaskPage(@Valid WorkflowTaskPageQuery queryParams) {
        Page<DoneTaskVO> result = workflowTaskService.getDoneTaskPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "获取待办任务详情（发起表单回显 + 审批记录）")
    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable String taskId
    ) {
        return Result.success(workflowTaskService.getTaskDetail(taskId));
    }

    @Operation(summary = "获取驳回目标节点列表")
    @GetMapping("/{taskId}/reject-targets")
    public Result<List<RejectTargetVO>> listRejectTargets(
            @Parameter(description = "任务ID") @PathVariable String taskId
    ) {
        return Result.success(workflowTaskService.listRejectTargets(taskId));
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{taskId}/complete")
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.UPDATE, title = "审批通过")
    public Result<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskForm formData
    ) {
        workflowTaskService.completeTask(taskId, formData);
        return Result.success();
    }

    @Operation(summary = "驳回")
    @PutMapping("/{taskId}/reject")
    @RepeatSubmit
    @Log(module = LogModuleEnum.WORKFLOW, value = ActionTypeEnum.UPDATE, title = "驳回")
    public Result<Void> rejectTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Valid @RequestBody RejectTaskForm formData
    ) {
        workflowTaskService.rejectTask(taskId, formData);
        return Result.success();
    }

    // AI 辅助：未开启 AI 时调用返回业务异常

    @Operation(summary = "AI 生成任务审批摘要")
    @PostMapping("/{taskId}/ai-summary")
    public Result<String> summarizeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId
    ) {
        return Result.success(workflowTaskService.aiSummarizeTask(taskId));
    }

}
