package com.youlai.boot.module.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.workflow.model.form.StartProcessForm;
import com.youlai.boot.module.workflow.model.query.WorkflowInstancePageQuery;
import com.youlai.boot.module.workflow.model.vo.InstanceDetailVO;
import com.youlai.boot.module.workflow.model.vo.InstancePageVO;
import com.youlai.boot.module.workflow.model.vo.ProcessDiagramVO;

/**
 * 流程实例业务接口（发起、我的流程、详情、流程图、撤销/终止）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface WorkflowInstanceService {

    /**
     * 发起流程（绑定表单时保存表单数据并以数据ID作为 businessKey，表单字段随流程变量提交）
     *
     * @param formData 发起流程表单数据
     * @return 流程实例ID
     */
    String startProcess(StartProcessForm formData);

    /**
     * 我的流程分页列表（当前用户发起，含运行中/已完成/已终止）
     *
     * @param queryParams 查询参数
     * @return 分页结果
     */
    Page<InstancePageVO> getInstancePage(WorkflowInstancePageQuery queryParams);

    /**
     * 获取流程图数据（BPMN XML + 已办/进行中节点，bpmn-js 高亮渲染）
     *
     * @param instanceId 流程实例ID
     * @return 流程图数据
     */
    ProcessDiagramVO getProcessDiagram(String instanceId);

    /**
     * 获取流程实例详情（发起表单回显 + 审批记录）
     *
     * @param instanceId 流程实例ID
     * @return 实例详情
     */
    InstanceDetailVO getInstanceDetail(String instanceId);

    /**
     * 撤销流程（仅发起人可撤销运行中实例）
     *
     * @param instanceId 流程实例ID
     */
    void cancelProcessInstance(String instanceId);

    /**
     * 终止流程（管理员终止任意运行中实例）
     *
     * @param instanceId 流程实例ID
     * @param reason     终止原因
     */
    void terminateProcessInstance(String instanceId, String reason);

}
