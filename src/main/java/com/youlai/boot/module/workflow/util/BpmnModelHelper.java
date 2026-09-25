package com.youlai.boot.module.workflow.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BPMN 模型读取辅助（formKey 与 XML 解析）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Component
@RequiredArgsConstructor
public class BpmnModelHelper {

    private final RepositoryService repositoryService;

    /**
     * 用户任务节点的 BPMN 原始信息（办理人为未翻译的原始配置）
     *
     * @param nodeId          节点ID
     * @param nodeName        节点名称
     * @param candidateGroups 候选组（角色编码）列表，可为空
     * @param assignee        办理人表达式（如 ${initiator}），可为空
     */
    public record UserTaskInfo(String nodeId, String nodeName, List<String> candidateGroups, String assignee) {
    }

    /**
     * 获取主流程的全部用户任务节点（BPMN 文档序，即流程模型中的节点编排顺序）
     *
     * @param processDefinitionId 流程定义ID
     */
    public List<UserTaskInfo> getUserTasks(String processDefinitionId) {
        Process process = getMainProcess(processDefinitionId);
        if (process == null) {
            return List.of();
        }
        return process.findFlowElementsOfType(UserTask.class).stream()
                .map(task -> new UserTaskInfo(task.getId(), task.getName(),
                        task.getCandidateGroups(), task.getAssignee()))
                .toList();
    }

    /**
     * 获取开始节点绑定的表单标识
     *
     * @param processDefinitionId 流程定义ID
     * @return formKey，未绑定为 null
     */
    public String getStartFormKey(String processDefinitionId) {
        Process process = getMainProcess(processDefinitionId);
        if (process == null) {
            return null;
        }
        return process.findFlowElementsOfType(StartEvent.class).stream()
                .findFirst()
                .map(StartEvent::getFormKey)
                .orElse(null);
    }

    /**
     * 获取用户任务节点绑定的表单标识
     *
     * @param processDefinitionId 流程定义ID
     * @param taskDefinitionKey   任务节点ID
     * @return formKey，未绑定或节点非用户任务为 null
     */
    public String getUserTaskFormKey(String processDefinitionId, String taskDefinitionKey) {
        Process process = getMainProcess(processDefinitionId);
        if (process == null) {
            return null;
        }
        return process.findFlowElementsOfType(UserTask.class).stream()
                .filter(task -> task.getId().equals(taskDefinitionKey))
                .findFirst()
                .map(UserTask::getFormKey)
                .orElse(null);
    }

    /**
     * 获取流程定义引用的全部表单标识（开始节点与用户任务节点，去重）
     *
     * @param processDefinitionId 流程定义ID
     * @return formKey 集合，未绑定返回空集合
     */
    public Set<String> getFormKeys(String processDefinitionId) {
        Process process = getMainProcess(processDefinitionId);
        if (process == null) {
            return Set.of();
        }
        return extractFormKeys(process);
    }

    /**
     * 提取 BPMN XML 引用的全部表单标识（开始节点与用户任务节点，去重）
     *
     * <p>供表单删除前校验使用：设计器草稿未部署，其引用不体现在流程定义里</p>
     *
     * @param bpmnXml BPMN 2.0 XML 字符串
     * @return formKey 集合，无引用返回空集合
     */
    public Set<String> getFormKeysFromXml(String bpmnXml) {
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, true);
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            throw new BusinessException("流程文件校验失败，未找到主流程");
        }
        return extractFormKeys(process);
    }

    /**
     * 提取主流程中开始节点与用户任务节点引用的表单标识
     */
    private Set<String> extractFormKeys(Process process) {
        Set<String> formKeys = new HashSet<>();
        process.findFlowElementsOfType(StartEvent.class).stream()
                .map(StartEvent::getFormKey).filter(StrUtil::isNotBlank).forEach(formKeys::add);
        process.findFlowElementsOfType(UserTask.class).stream()
                .map(UserTask::getFormKey).filter(StrUtil::isNotBlank).forEach(formKeys::add);
        return formKeys;
    }

    /**
     * 获取流程定义的 BPMN XML
     *
     * @param processDefinitionId 流程定义ID
     * @return BPMN 2.0 XML 字符串
     */
    public String getBpmnXml(String processDefinitionId) {
        try (InputStream in = repositoryService.getProcessModel(processDefinitionId)) {
            if (in == null) {
                throw new BusinessException("流程图不存在");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("流程图读取失败");
        }
    }

    /**
     * 校验 BPMN XML 中用户任务节点的办理人配置
     *
     * <p>assignee/候选组/候选用户三者均未配置的任务会成为无主任务：
     * 不出现在任何人的待办列表，流程将永久卡在该节点</p>
     *
     * @param bpmnXml BPMN 2.0 XML 字符串
     * @return 未配置办理人的节点名称列表（全部已配置返回空列表）
     */
    public List<String> getUnassignedTaskNames(String bpmnXml) {
        // validateSchema=false：bpmn-js 产出的命名空间前缀未必严格符合 XSD，引擎部署同样不强制
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, true);
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            throw new BusinessException("流程文件校验失败，未找到主流程");
        }
        List<String> unassigned = new ArrayList<>();
        for (UserTask task : process.findFlowElementsOfType(UserTask.class)) {
            boolean noAssignee = StrUtil.isBlank(task.getAssignee());
            boolean noGroups = CollUtil.isEmpty(task.getCandidateGroups());
            boolean noUsers = CollUtil.isEmpty(task.getCandidateUsers());
            if (noAssignee && noGroups && noUsers) {
                unassigned.add(task.getName());
            }
        }
        return unassigned;
    }

    /**
     * 提取 BPMN XML 中全部用户任务引用的候选组编码（去重）
     *
     * <p>供发布校验核对角色存在性：候选组指向不存在的角色时任务无人可见，同样会卡死流程</p>
     *
     * @param bpmnXml BPMN 2.0 XML 字符串
     * @return 候选组角色编码集合（无候选组返回空集合）
     */
    public Set<String> getCandidateGroups(String bpmnXml) {
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(
                () -> new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)), false, true);
        Process process = bpmnModel.getMainProcess();
        if (process == null) {
            throw new BusinessException("流程文件校验失败，未找到主流程");
        }
        return process.findFlowElementsOfType(UserTask.class).stream()
                .flatMap(task -> task.getCandidateGroups().stream())
                .collect(Collectors.toSet());
    }

    /**
     * 获取主流程（泳道等子流程不参与 formKey 解析）
     */
    private Process getMainProcess(String processDefinitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId).singleResult();
        if (definition == null) {
            throw new BusinessException("流程定义不存在");
        }
        return repositoryService.getBpmnModel(processDefinitionId).getMainProcess();
    }

}
