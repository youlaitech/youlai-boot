package com.youlai.boot.module.workflow.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.module.workflow.model.form.DefinitionStateForm;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;
import com.youlai.boot.module.workflow.model.vo.ProcessStageVO;
import com.youlai.boot.module.workflow.model.vo.StartableProcessVO;
import com.youlai.boot.module.workflow.service.WorkflowDefinitionService;
import com.youlai.boot.module.workflow.util.BpmnModelHelper;
import com.youlai.boot.system.model.entity.Role;
import com.youlai.boot.system.model.entity.SysUser;
import com.youlai.boot.system.model.entity.UserRole;
import com.youlai.boot.system.service.RoleService;
import com.youlai.boot.system.service.UserService;
import com.youlai.boot.system.service.UserRoleService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程定义业务实现类
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private final RepositoryService repositoryService;
    private final BpmnModelHelper bpmnModelHelper;
    private final RoleService roleService;
    private final UserService userService;
    private final UserRoleService userRoleService;

    /**
     * 可发起流程列表
     */
    @Override
    public List<StartableProcessVO> listStartableProcesses() {
        return repositoryService.createProcessDefinitionQuery().latestVersion().active()
                .orderByProcessDefinitionKey().asc().list().stream()
                .map(definition -> {
                    StartableProcessVO processVO = new StartableProcessVO();
                    processVO.setId(definition.getId());
                    processVO.setKey(definition.getKey());
                    processVO.setName(definition.getName());
                    processVO.setVersion(definition.getVersion());
                    processVO.setFormKey(bpmnModelHelper.getStartFormKey(definition.getId()));
                    return processVO;
                }).toList();
    }

    /**
     * 流程审批阶段预览：BPMN 节点的候选组（角色编码）与办理人表达式翻译为角色名和角色成员账号，
     * 让发起人在提交前即可看到"哪个环节由谁审批"
     */
    @Override
    public List<ProcessStageVO> listProcessStages(String definitionId) {
        getDefinition(definitionId);
        List<BpmnModelHelper.UserTaskInfo> userTasks = bpmnModelHelper.getUserTasks(definitionId);
        if (userTasks.isEmpty()) {
            return List.of();
        }

        // 涉及角色一次性批量解析（角色名 + 成员账号），避免逐节点回查
        Set<String> roleCodes = userTasks.stream()
                .flatMap(task -> task.candidateGroups().stream())
                .collect(Collectors.toSet());
        RoleDirectory directory = loadRoleDirectory(roleCodes);

        return userTasks.stream().map(task -> {
            // 候选组任务的办理人：角色成员账号；字面量 assignee（固定用户）与未知表达式原样展示
            List<String> userNames = new ArrayList<>(task.candidateGroups().stream()
                    .flatMap(code -> directory.roleUsers().getOrDefault(code, List.of()).stream())
                    .distinct()
                    .toList());
            boolean byInitiator = StrUtil.isNotBlank(task.assignee())
                    && task.assignee().contains("initiator");
            if (StrUtil.isNotBlank(task.assignee()) && !byInitiator) {
                userNames.add(task.assignee());
            }
            return ProcessStageVO.builder()
                    .nodeId(task.nodeId())
                    .nodeName(task.nodeName())
                    // 角色已删除或未初始化时回退展示编码，环节不因数据缺失而空白
                    .roleNames(task.candidateGroups().stream()
                            .map(code -> directory.roleNames().getOrDefault(code, code))
                            .toList())
                    .userNames(userNames)
                    .initiator(byInitiator)
                    .build();
        }).toList();
    }

    /**
     * 角色目录（编码 → 角色名 / 成员账号）
     *
     * @param roleNames 编码 → 角色名称
     * @param roleUsers 编码 → 成员账号列表
     */
    private record RoleDirectory(Map<String, String> roleNames, Map<String, List<String>> roleUsers) {
    }

    /**
     * 批量解析角色名称与成员账号（角色 → 用户ID → 账号 三段批查）
     */
    private RoleDirectory loadRoleDirectory(Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return new RoleDirectory(Map.of(), Map.of());
        }
        List<Role> roles = roleService.list(
                new LambdaQueryWrapper<Role>().in(Role::getCode, roleCodes));
        if (roles.isEmpty()) {
            return new RoleDirectory(Map.of(), Map.of());
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        Map<Long, List<Long>> userIdsByRole = userRoleService.list(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, roleIds))
                .stream().collect(Collectors.groupingBy(UserRole::getRoleId,
                        Collectors.mapping(UserRole::getUserId, Collectors.toList())));
        Set<Long> userIds = userIdsByRole.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> usernameById = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        Map<String, List<String>> roleUsers = new HashMap<>();
        roles.forEach(role -> roleUsers.put(role.getCode(),
                userIdsByRole.getOrDefault(role.getId(), List.of()).stream()
                        .map(usernameById::get)
                        .filter(StrUtil::isNotBlank)
                        .toList()));
        Map<String, String> roleNames = roles.stream()
                .collect(Collectors.toMap(Role::getCode, Role::getName));
        return new RoleDirectory(roleNames, roleUsers);
    }

    /**
     * 获取流程定义 BPMN XML
     *
     * @param definitionId 流程定义ID
     */
    @Override
    public ModelXmlVO getDefinitionXml(String definitionId) {
        getDefinition(definitionId);
        return new ModelXmlVO(bpmnModelHelper.getBpmnXml(definitionId));
    }

    /**
     * 修改流程定义状态
     *
     * @param definitionId 流程定义ID
     * @param formData     状态表单数据
     */
    @Override
    public void updateDefinitionState(String definitionId, DefinitionStateForm formData) {
        getDefinition(definitionId);
        if (Boolean.TRUE.equals(formData.getSuspend())) {
            repositoryService.suspendProcessDefinitionById(definitionId);
        } else {
            repositoryService.activateProcessDefinitionById(definitionId);
        }
    }

    /**
     * 获取流程定义（不存在即抛业务异常）
     */
    private ProcessDefinition getDefinition(String definitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        Assert.notNull(definition, "流程定义不存在");
        return definition;
    }

}
