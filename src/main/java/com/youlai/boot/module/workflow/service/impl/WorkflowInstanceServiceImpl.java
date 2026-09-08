package com.youlai.boot.module.workflow.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.workflow.enums.InstanceStatusEnum;
import com.youlai.boot.module.workflow.model.form.StartProcessForm;
import com.youlai.boot.module.workflow.model.query.WorkflowInstancePageQuery;
import com.youlai.boot.module.workflow.model.vo.InstanceDetailVO;
import com.youlai.boot.module.workflow.model.vo.InstancePageVO;
import com.youlai.boot.module.workflow.model.vo.ProcessDiagramVO;
import com.youlai.boot.module.workflow.model.vo.ProcessStageVO;
import com.youlai.boot.module.workflow.service.WorkflowDefinitionService;
import com.youlai.boot.module.workflow.service.WorkflowInstanceService;
import com.youlai.boot.module.workflow.util.ApprovalHistoryHelper;
import com.youlai.boot.module.workflow.util.BpmnModelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程实例业务实现类
 *
 * <p>发起时绑定动态表单：表单数据落库后以数据ID作为 businessKey，表单字段随流程变量提交，
 * 网关条件可直接引用表单字段（如 ${days > 3}）</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceServiceImpl implements WorkflowInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final FormDataService formDataService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final BpmnModelHelper bpmnModelHelper;
    private final ApprovalHistoryHelper approvalHistoryHelper;

    /**
     * 发起流程
     *
     * @param formData 发起流程表单数据
     */
    @Override
    public String startProcess(StartProcessForm formData) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(formData.getProcessDefinitionId()).singleResult();
        Assert.notNull(definition, "流程定义不存在");
        Assert.isTrue(!definition.isSuspended(), "流程定义已挂起，无法发起");

        Map<String, Object> variables = new HashMap<>();
        String businessKey = null;
        String formKey = bpmnModelHelper.getStartFormKey(definition.getId());
        if (StrUtil.isNotBlank(formKey)) {
            Map<String, Object> data = formData.getFormData() == null ? Map.of() : formData.getFormData();
            Long dataId = formDataService.saveFormDataReturnId(formKey, data);
            Assert.notNull(dataId, "表单数据保存失败");
            businessKey = String.valueOf(dataId);
            // 表单字段即流程变量，网关条件按字段名直接引用
            variables.putAll(data);
        }
        if (formData.getVariables() != null) {
            variables.putAll(formData.getVariables());
        }

        // 发起人写入 ACT_HI_PROCINST.START_USER_ID_，供"我的流程"与发起人撤销校验使用
        Authentication.setAuthenticatedUserId(SecurityUtils.getUsername());
        try {
            // 实例名：未传时回退流程定义名（实例级 NAME_，区分同流程的多次发起）
            String instanceName = StrUtil.blankToDefault(formData.getName(), definition.getName());
            // RuntimeService 无 (id, businessKey, name, variables) 重载，实例名须走 builder 链式构建
            ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionId(definition.getId())
                    .businessKey(businessKey)
                    .name(instanceName)
                    .variables(variables)
                    .start();
            return instance.getId();
        } finally {
            Authentication.setAuthenticatedUserId(null);
        }
    }

    /**
     * 我的流程分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<InstancePageVO> getInstancePage(WorkflowInstancePageQuery queryParams) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .startedBy(SecurityUtils.getUsername());
        if (StrUtil.isNotBlank(queryParams.getKeywords())) {
            query.processDefinitionNameLike("%" + queryParams.getKeywords() + "%");
        }
        query.orderByProcessInstanceStartTime().desc();
        long total = query.count();
        List<HistoricProcessInstance> instances = query.listPage(
                (queryParams.getPageNum() - 1) * queryParams.getPageSize(), queryParams.getPageSize());

        // 同一定义只解析一次 formKey
        Map<String, String> formKeys = new HashMap<>();
        List<InstancePageVO> records = instances.stream().map(instance -> {
            InstancePageVO pageVO = new InstancePageVO();
            pageVO.setId(instance.getId());
            pageVO.setProcessDefinitionKey(instance.getProcessDefinitionKey());
            // 实例名优先（发起时可自定义标题区分批次），旧实例 NAME_ 为空则回退流程定义名
            pageVO.setProcessName(StrUtil.isBlank(instance.getName())
                    ? instance.getProcessDefinitionName() : instance.getName());
            pageVO.setBusinessKey(instance.getBusinessKey());
            pageVO.setFormKey(formKeys.computeIfAbsent(instance.getProcessDefinitionId(),
                    bpmnModelHelper::getStartFormKey));
            pageVO.setStatus(resolveStatus(instance));
            pageVO.setDeleteReason(instance.getDeleteReason());
            pageVO.setStartTime(instance.getStartTime());
            pageVO.setEndTime(instance.getEndTime());
            return pageVO;
        }).toList();

        Page<InstancePageVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize(), total);
        page.setRecords(records);
        return page;
    }

    /**
     * 获取流程图数据
     *
     * @param instanceId 流程实例ID
     */
    @Override
    public ProcessDiagramVO getProcessDiagram(String instanceId) {
        HistoricProcessInstance instance = getHistoricInstance(instanceId);
        ProcessDiagramVO diagramVO = new ProcessDiagramVO();
        diagramVO.setBpmnXml(bpmnModelHelper.getBpmnXml(instance.getProcessDefinitionId()));
        diagramVO.setExecutedActivityIds(historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId).finished().list().stream()
                .map(HistoricActivityInstance::getActivityId).distinct().toList());
        // 已结束实例无进行中节点，查询会因执行流不存在而报错
        diagramVO.setActiveActivityIds(instance.getEndTime() == null
                ? runtimeService.getActiveActivityIds(instanceId) : List.of());
        return diagramVO;
    }

    /**
     * 获取流程实例详情
     *
     * @param instanceId 流程实例ID
     */
    @Override
    public InstanceDetailVO getInstanceDetail(String instanceId) {
        HistoricProcessInstance instance = getHistoricInstance(instanceId);
        InstanceDetailVO detailVO = new InstanceDetailVO();
        detailVO.setId(instance.getId());
        // 实例名优先（与列表保持一致），旧实例 NAME_ 为空则回退流程定义名
        detailVO.setProcessName(StrUtil.isBlank(instance.getName())
                ? instance.getProcessDefinitionName() : instance.getName());
        detailVO.setStartUser(instance.getStartUserId());
        detailVO.setStatus(resolveStatus(instance));
        detailVO.setDeleteReason(instance.getDeleteReason());
        detailVO.setStartTime(instance.getStartTime());
        detailVO.setEndTime(instance.getEndTime());

        String formKey = bpmnModelHelper.getStartFormKey(instance.getProcessDefinitionId());
        detailVO.setFormKey(formKey);
        this.fillFormData(detailVO, formKey, instance.getBusinessKey());
        detailVO.setHistory(approvalHistoryHelper.getHistory(instanceId));

        // 流程走向预览：发起人视角直观看到流程走到哪个环节
        List<ProcessStageVO> stages = workflowDefinitionService.listProcessStages(
                instance.getProcessDefinitionId());
        detailVO.setStages(stages);
        detailVO.setActiveStageIndex(resolveActiveStageIndex(instance, stages));
        return detailVO;
    }

    /**
     * 计算流程走向的高亮位置：运行中取当前活动节点，正常结束为全部完成，
     * 终止/撤销取最后办完环节的下一位置（已办环节仍显示完成态）
     */
    private int resolveActiveStageIndex(HistoricProcessInstance instance, List<ProcessStageVO> stages) {
        if (stages.isEmpty()) {
            return 0;
        }
        // 正常结束：全部环节完成
        if (instance.getEndTime() != null && StrUtil.isBlank(instance.getDeleteReason())) {
            return stages.size();
        }
        if (instance.getEndTime() == null) {
            // 运行中：第一个活动节点即为进行中的环节
            Set<String> activeActivityIds = new HashSet<>(runtimeService.getActiveActivityIds(instance.getId()));
            return stages.stream()
                    .filter(stage -> activeActivityIds.contains(stage.getNodeId()))
                    .findFirst()
                    .map(stages::indexOf)
                    .orElse(0);
        }
        // 终止/撤销：已办环节保持完成态，高亮停在最后办完环节之后
        return approvalHistoryHelper.getHistory(instance.getId()).stream()
                .filter(record -> record.getEndTime() != null)
                .reduce((first, second) -> second)
                .flatMap(last -> stages.stream()
                        .filter(stage -> stage.getNodeName().equals(last.getActivityName()))
                        .findFirst())
                .map(stages::indexOf)
                .map(index -> index + 1)
                .orElse(0);
    }

    /**
     * 撤销流程
     *
     * @param instanceId 流程实例ID
     */
    @Override
    public void cancelProcessInstance(String instanceId) {
        HistoricProcessInstance instance = getHistoricInstance(instanceId);
        Assert.isTrue(instance.getEndTime() == null, "流程已结束，无法撤销");
        Assert.isTrue(SecurityUtils.getUsername().equals(instance.getStartUserId()), "仅发起人可撤销流程");
        runtimeService.deleteProcessInstance(instanceId, "发起人撤销");
    }

    /**
     * 终止流程
     *
     * @param instanceId 流程实例ID
     * @param reason     终止原因
     */
    @Override
    public void terminateProcessInstance(String instanceId, String reason) {
        HistoricProcessInstance instance = getHistoricInstance(instanceId);
        Assert.isTrue(instance.getEndTime() == null, "流程已结束，无法终止");
        runtimeService.deleteProcessInstance(instanceId, StrUtil.blankToDefault(reason, "管理员终止"));
    }

    /**
     * 发起表单数据回显（按提交时快照严格回显）
     *
     * <p>表单数据缺失时静默降级：业务表重装（dynamic-form.sql 重建）而引擎表保留、
     * 或数据被手动删除时实例仍在，详情页可继续展示审批记录/流程走向/流程图，不因表单缺失整体报错</p>
     */
    private void fillFormData(InstanceDetailVO detailVO, String formKey, String businessKey) {
        if (StrUtil.isBlank(formKey) || StrUtil.isBlank(businessKey)) {
            return;
        }
        // 非数字 businessKey（未经过本系统表单提交发起）无表单可回显
        if (!NumberUtil.isLong(businessKey)) {
            log.warn("跳过表单回显：businessKey 非表单数据ID  instanceId={} businessKey={}", detailVO.getId(), businessKey);
            return;
        }
        FormDataVO dataVO = formDataService.getFormDataDetail(Long.valueOf(businessKey));
        if (dataVO == null) {
            log.warn("发起表单数据不存在，跳过回显  instanceId={} businessKey={}", detailVO.getId(), businessKey);
            return;
        }
        detailVO.setFormJson(dataVO.getFormJson());
        detailVO.setOptionsJson(dataVO.getOptionsJson());
        detailVO.setDataJson(dataVO.getDataJson());
    }

    /**
     * 实例状态映射：未结束即运行中；有终止原因为已终止；其余为已完成
     */
    private InstanceStatusEnum resolveStatus(HistoricProcessInstance instance) {
        if (instance.getEndTime() == null) {
            return InstanceStatusEnum.RUNNING;
        }
        return StrUtil.isNotBlank(instance.getDeleteReason())
                ? InstanceStatusEnum.TERMINATED : InstanceStatusEnum.FINISHED;
    }

    /**
     * 获取历史流程实例（不存在即抛业务异常）
     */
    private HistoricProcessInstance getHistoricInstance(String instanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult();
        Assert.notNull(instance, "流程实例不存在");
        return instance;
    }

}
