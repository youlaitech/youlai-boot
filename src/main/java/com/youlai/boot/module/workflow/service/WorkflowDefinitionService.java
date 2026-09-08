package com.youlai.boot.module.workflow.service;

import com.youlai.boot.module.workflow.model.form.DefinitionStateForm;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;
import com.youlai.boot.module.workflow.model.vo.ProcessStageVO;
import com.youlai.boot.module.workflow.model.vo.StartableProcessVO;

import java.util.List;

/**
 * 流程定义业务接口（发布产物：可发起列表、审批阶段、流程图、启用停用）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface WorkflowDefinitionService {

    /**
     * 可发起流程列表（最新版本且激活，发起页下拉数据源）
     *
     * @return 流程定义集合
     */
    List<StartableProcessVO> listStartableProcesses();

    /**
     * 流程审批阶段预览（按 BPMN 编排顺序解析各环节的审批角色与角色成员）
     *
     * @param definitionId 流程定义ID
     * @return 审批阶段集合
     */
    List<ProcessStageVO> listProcessStages(String definitionId);

    /**
     * 获取流程定义 BPMN XML（流程图查看）
     *
     * @param definitionId 流程定义ID
     * @return BPMN 2.0 XML
     */
    ModelXmlVO getDefinitionXml(String definitionId);

    /**
     * 启用/停用流程（停用后不可发起新实例，存量实例不受影响）
     *
     * @param definitionId 流程定义ID
     * @param formData     状态表单数据
     */
    void updateDefinitionState(String definitionId, DefinitionStateForm formData);

}
