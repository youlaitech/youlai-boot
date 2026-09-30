package com.youlai.boot.module.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.form.util.FormRuleValidator;
import com.youlai.boot.module.workflow.model.form.CompleteTaskForm;
import com.youlai.boot.module.workflow.model.form.RejectTaskForm;
import com.youlai.boot.module.workflow.model.query.WorkflowTaskPageQuery;
import com.youlai.boot.module.workflow.model.vo.ApprovalHistoryVO;
import com.youlai.boot.module.workflow.model.vo.DoneTaskVO;
import com.youlai.boot.module.workflow.model.vo.RejectTargetVO;
import com.youlai.boot.module.workflow.model.vo.TaskDetailVO;
import com.youlai.boot.module.workflow.model.vo.TodoTaskVO;
import com.youlai.boot.module.workflow.service.WorkflowDefinitionService;
import com.youlai.boot.module.workflow.service.WorkflowTaskService;
import com.youlai.boot.module.workflow.util.ApprovalHistoryHelper;
import com.youlai.boot.module.workflow.util.BpmnModelHelper;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import com.youlai.boot.support.ai.AiChatService;
import com.youlai.boot.support.ai.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程任务业务实现类
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Slf4j
@ConditionalOnWorkflowEnabled
@Service
@RequiredArgsConstructor
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    // 系统提示词名称，对应 resources/prompts 下的相对路径
    private static final String SYSTEM_PROMPT_PATH = "workflow/summary.md";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final FormDataService formDataService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final BpmnModelHelper bpmnModelHelper;
    private final ApprovalHistoryHelper approvalHistoryHelper;
    private final AiChatService aiChatService;
    private final PromptLoader promptLoader;

    /**
     * 待办任务分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<TodoTaskVO> getTodoTaskPage(WorkflowTaskPageQuery queryParams) {
        TaskQuery query = this.createCandidateTaskQuery();
        if (StrUtil.isNotBlank(queryParams.getKeywords())) {
            query.taskNameLike("%" + queryParams.getKeywords() + "%");
        }
        query.orderByTaskCreateTime().desc();
        long total = query.count();
        List<Task> tasks = query.listPage(
                (queryParams.getPageNum() - 1) * queryParams.getPageSize(), queryParams.getPageSize());

        // 流程名称、businessKey 不在任务表上，批量回查避免逐行查询
        Map<String, String> processNames = getProcessNames(
                tasks.stream().map(Task::getProcessDefinitionId).toList());
        Map<String, ProcessInstance> instances = getProcessInstances(
                tasks.stream().map(Task::getProcessInstanceId).collect(Collectors.toSet()));
        Map<String, String> formKeys = new HashMap<>();

        List<TodoTaskVO> records = tasks.stream().map(task -> {
            TodoTaskVO taskVO = new TodoTaskVO();
            taskVO.setId(task.getId());
            taskVO.setName(task.getName());
            taskVO.setProcessInstanceId(task.getProcessInstanceId());
            ProcessInstance instance = instances.get(task.getProcessInstanceId());
            taskVO.setProcessName(resolveProcessName(
                    instance == null ? null : instance.getName(),
                    processNames.get(task.getProcessDefinitionId())));
            taskVO.setBusinessKey(instance == null ? null : instance.getBusinessKey());
            taskVO.setFormKey(formKeys.computeIfAbsent(task.getProcessDefinitionId(),
                    bpmnModelHelper::getStartFormKey));
            taskVO.setCreateTime(task.getCreateTime());
            return taskVO;
        }).toList();

        Page<TodoTaskVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize(), total);
        page.setRecords(records);
        return page;
    }

    /**
     * 已办任务分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<DoneTaskVO> getDoneTaskPage(WorkflowTaskPageQuery queryParams) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(SecurityUtils.getUsername()).finished();
        if (StrUtil.isNotBlank(queryParams.getKeywords())) {
            query.taskNameLike("%" + queryParams.getKeywords() + "%");
        }
        query.orderByHistoricTaskInstanceEndTime().desc();
        long total = query.count();
        List<HistoricTaskInstance> tasks = query.listPage(
                (queryParams.getPageNum() - 1) * queryParams.getPageSize(), queryParams.getPageSize());

        Map<String, String> processNames = getProcessNames(
                tasks.stream().map(HistoricTaskInstance::getProcessDefinitionId).toList());
        // 已办实例可能已结束，运行时表查不到，实例名须从历史表批量取（key 为流程实例ID）
        Map<String, HistoricProcessInstance> instances = getHistoricProcessInstances(
                tasks.stream().map(HistoricTaskInstance::getProcessInstanceId).collect(Collectors.toSet()));
        // 意见存于 ACT_HI_COMMENT（按流程实例归档），分页内按实例去重批量回查，避免逐任务查询
        Map<String, String> comments = new HashMap<>();
        tasks.stream().map(HistoricTaskInstance::getProcessInstanceId).distinct()
                .forEach(processInstanceId -> taskService.getProcessInstanceComments(processInstanceId)
                        .forEach(comment -> {
                            if (StrUtil.isNotBlank(comment.getTaskId())) {
                                comments.put(comment.getTaskId(), comment.getFullMessage());
                            }
                        }));
        List<DoneTaskVO> records = tasks.stream().map(task -> {
            DoneTaskVO taskVO = new DoneTaskVO();
            taskVO.setId(task.getId());
            taskVO.setName(task.getName());
            taskVO.setProcessInstanceId(task.getProcessInstanceId());
            HistoricProcessInstance instance = instances.get(task.getProcessInstanceId());
            taskVO.setProcessName(resolveProcessName(
                    instance == null ? null : instance.getName(),
                    processNames.get(task.getProcessDefinitionId())));
            taskVO.setEndTime(task.getEndTime());
            taskVO.setComment(comments.get(task.getId()));
            return taskVO;
        }).toList();

        Page<DoneTaskVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize(), total);
        page.setRecords(records);
        return page;
    }

    /**
     * 获取待办任务详情
     *
     * @param taskId 任务ID
     */
    @Override
    public TaskDetailVO getTaskDetail(String taskId) {
        Task task = getCurrentUserTask(taskId);
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
        Assert.notNull(instance, "流程实例不存在");

        TaskDetailVO detailVO = new TaskDetailVO();
        detailVO.setTaskId(task.getId());
        detailVO.setTaskName(task.getName());
        detailVO.setTaskDefinitionKey(task.getTaskDefinitionKey());
        detailVO.setProcessInstanceId(task.getProcessInstanceId());
        detailVO.setProcessName(resolveProcessName(instance.getName(), instance.getProcessDefinitionName()));
        detailVO.setTaskFormKey(bpmnModelHelper.getUserTaskFormKey(
                task.getProcessDefinitionId(), task.getTaskDefinitionKey()));

        String formKey = bpmnModelHelper.getStartFormKey(task.getProcessDefinitionId());
        detailVO.setFormKey(formKey);
        this.fillFormData(detailVO, formKey, instance.getBusinessKey());
        detailVO.setHistory(approvalHistoryHelper.getHistory(task.getProcessInstanceId()));
        // 流程走向预览（审批人办理前可见完整审批链，明确前后环节办理人）
        detailVO.setStages(workflowDefinitionService.listProcessStages(task.getProcessDefinitionId()));
        return detailVO;
    }

    /**
     * 获取驳回目标节点列表
     *
     * @param taskId 任务ID
     */
    @Override
    public List<RejectTargetVO> listRejectTargets(String taskId) {
        Task task = getCurrentUserTask(taskId);
        // 已办节点去重取首个名称；驳回当前节点自身无意义，排除
        Map<String, String> targets = new LinkedHashMap<>();
        historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceStartTime().asc()
                .list()
                .forEach(activity -> {
                    if (!activity.getActivityId().equals(task.getTaskDefinitionKey())) {
                        targets.putIfAbsent(activity.getActivityId(), activity.getActivityName());
                    }
                });
        return targets.entrySet().stream()
                .map(entry -> new RejectTargetVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 审批通过
     *
     * @param taskId   任务ID
     * @param formData 审批表单数据
     */
    @Override
    public void completeTask(String taskId, CompleteTaskForm formData) {
        Task task = getCurrentUserTask(taskId);
        this.addComment(task, formData.getComment());
        this.claimIfCandidate(task);
        taskService.complete(taskId, formData.getVariables() == null ? Map.of() : formData.getVariables());
    }

    /**
     * 驳回
     *
     * @param taskId   任务ID
     * @param formData 驳回表单数据
     */
    @Override
    public void rejectTask(String taskId, RejectTaskForm formData) {
        Task task = getCurrentUserTask(taskId);
        // 目标必须是本实例已办理过的节点，防止驳回到未走过的分支
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .activityId(formData.getTargetActivityId())
                .finished()
                .count();
        Assert.isTrue(count > 0, "驳回目标必须是本实例已办理过的节点");

        this.addComment(task, formData.getComment());
        this.claimIfCandidate(task);
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(task.getExecutionId(), formData.getTargetActivityId())
                .changeState();
    }

    /**
     * 获取当前用户的待办任务（不存在或非本人待办即抛业务异常）
     */
    private Task getCurrentUserTask(String taskId) {
        Task task = this.createCandidateTaskQuery()
                .taskId(taskId)
                .singleResult();
        Assert.notNull(task, "任务不存在或非你的待办");
        return task;
    }

    /**
     * 当前用户的候选任务查询：本人任务（办理人/候选用户）或所属角色候选组的任务
     */
    private TaskQuery createCandidateTaskQuery() {
        String username = SecurityUtils.getUsername();
        List<String> roles = new ArrayList<>(SecurityUtils.getRoles());
        if (roles.isEmpty()) {
            return taskService.createTaskQuery()
                    .active().taskCandidateOrAssigned(username);
        }
        return taskService.createTaskQuery()
                .active().or()
                .taskCandidateOrAssigned(username)
                .taskCandidateGroupIn(roles)
                .endOr();
    }

    /**
     * 候选组任务办理前自动认领（认领后写入办理人，已办列表与审批记录可追溯）
     */
    private void claimIfCandidate(Task task) {
        if (task.getAssignee() == null) {
            taskService.claim(task.getId(), SecurityUtils.getUsername());
        }
    }

    /**
     * 审批意见写入引擎评论（发起人上下文决定评论作者）
     */
    private void addComment(Task task, String comment) {
        if (StrUtil.isBlank(comment)) {
            return;
        }
        Authentication.setAuthenticatedUserId(SecurityUtils.getUsername());
        try {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), comment);
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }
    }

    /**
     * 发起表单数据回显（按提交时快照严格回显）
     */
    private void fillFormData(TaskDetailVO detailVO, String formKey, String businessKey) {
        if (StrUtil.isBlank(formKey) || StrUtil.isBlank(businessKey)) {
            return;
        }
        // 非数字 businessKey（未经过本系统表单提交发起）无表单可回显
        if (!NumberUtil.isLong(businessKey)) {
            log.warn("跳过表单回显：businessKey 非表单数据ID  taskId={} businessKey={}", detailVO.getTaskId(), businessKey);
            return;
        }
        FormDataVO dataVO = formDataService.getFormDataDetail(Long.valueOf(businessKey));
        if (dataVO == null) {
            log.warn("发起表单数据不存在，跳过回显  taskId={} businessKey={}", detailVO.getTaskId(), businessKey);
            return;
        }
        detailVO.setFormJson(dataVO.getFormJson());
        detailVO.setOptionsJson(dataVO.getOptionsJson());
        detailVO.setDataJson(dataVO.getDataJson());
    }

    /**
     * 批量取流程定义名称（key 为流程定义ID）
     */
    private Map<String, String> getProcessNames(List<String> processDefinitionIds) {
        return processDefinitionIds.stream().distinct()
                .collect(Collectors.toMap(id -> id, id -> {
                    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionId(id).singleResult();
                    return definition == null ? "" : definition.getName();
                }));
    }

    /**
     * 解析流程名称：实例级名称优先（发起时可自定义标题区分同流程多次发起），旧实例 NAME_ 为空回退流程定义名
     */
    private String resolveProcessName(String instanceName, String definitionName) {
        return StrUtil.isBlank(instanceName) ? definitionName : instanceName;
    }

    /**
     * 批量取历史流程实例（key 为流程实例ID；已办任务对应的实例可能已结束，须查历史表）
     */
    private Map<String, HistoricProcessInstance> getHistoricProcessInstances(Set<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIds).list().stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, instance -> instance));
    }

    /**
     * 批量取运行中流程实例（key 为流程实例ID）
     */
    private Map<String, ProcessInstance> getProcessInstances(Set<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceIds(processInstanceIds).list().stream()
                .collect(Collectors.toMap(ProcessInstance::getId, instance -> instance));
    }

    //---------------------------------------------------
    // AI 辅助：未开启 AI 时调用返回业务异常
    //---------------------------------------------------

    /**
     * 用 AI 汇总表单数据与审批记录，生成审批摘要
     *
     * @param taskId 任务ID
     * @return 摘要文本
     */
    @Override
    public String aiSummarizeTask(String taskId) {
        TaskDetailVO detail = getTaskDetail(taskId);
        return aiChatService.chat(promptLoader.get(SYSTEM_PROMPT_PATH), buildAiUserPrompt(detail));
    }

    /**
     * 构造用户提示词
     *
     * @param detail 任务详情
     * @return 用户提示词
     */
    private String buildAiUserPrompt(TaskDetailVO detail) {
        StringBuilder text = new StringBuilder();
        text.append("流程名称：").append(StrUtil.blankToDefault(detail.getProcessName(), "未命名流程")).append("\n");
        text.append("当前节点：").append(StrUtil.blankToDefault(detail.getTaskName(), "未命名节点")).append("\n");
        text.append("表单数据：\n").append(buildAiFormDataText(detail));
        if (CollectionUtil.isNotEmpty(detail.getHistory())) {
            text.append("审批记录：\n");
            for (ApprovalHistoryVO history : detail.getHistory()) {
                text.append("- ")
                        .append(StrUtil.blankToDefault(history.getActivityName(), "环节"))
                        .append("（")
                        .append(StrUtil.blankToDefault(history.getAssignee(), "待处理"))
                        .append("）：")
                        .append(StrUtil.blankToDefault(history.getComment(), "无意见"))
                        .append("\n");
            }
        }
        return text.toString();
    }

    /**
     * 把表单数据按 rule 的中文标题输出，便于模型理解字段含义
     *
     * @param detail 任务详情
     * @return 逐行「标题：值」文本
     */
    private String buildAiFormDataText(TaskDetailVO detail) {
        Map<String, String> titles = FormRuleValidator.resolveTitles(detail.getFormJson());
        if (StrUtil.isBlank(detail.getDataJson())) {
            return "（无表单数据）\n";
        }
        StringBuilder text = new StringBuilder();
        try {
            JsonNode data = OBJECT_MAPPER.readTree(detail.getDataJson());
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String title = titles.getOrDefault(entry.getKey(), entry.getKey());
                text.append(title).append("：").append(entry.getValue().asText()).append("\n");
            }
        } catch (Exception e) {
            throw new BusinessException("表单数据解析失败");
        }
        return text.toString();
    }

}
