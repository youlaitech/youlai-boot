package com.youlai.boot.module.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.workflow.model.form.ModelForm;
import com.youlai.boot.module.workflow.model.form.ModelXmlForm;
import com.youlai.boot.module.workflow.model.query.WorkflowModelPageQuery;
import com.youlai.boot.module.workflow.model.vo.ModelVO;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;
import com.youlai.boot.module.workflow.service.WorkflowModelService;
import com.youlai.boot.module.workflow.util.BpmnModelHelper;
import com.youlai.boot.system.model.entity.Role;
import com.youlai.boot.system.service.RoleService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 流程模型业务实现类
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowModelServiceImpl implements WorkflowModelService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final BpmnModelHelper bpmnModelHelper;
    private final RoleService roleService;

    // 新模型初始模板：开始 → 审批 → 结束，设计器打开即可直接编辑
    private static final String INITIAL_XML_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                              xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                              xmlns:flowable="http://flowable.org/bpmn"
                              id="Definitions_%1$s" targetNamespace="http://flowable.org/bpmn">
              <bpmn:process id="%1$s" name="%2$s" isExecutable="true">
                <bpmn:startEvent id="startEvent" name="开始">
                  <bpmn:outgoing>flow1</bpmn:outgoing>
                </bpmn:startEvent>
                <bpmn:userTask id="activity1" name="审批">
                  <bpmn:incoming>flow1</bpmn:incoming>
                  <bpmn:outgoing>flow2</bpmn:outgoing>
                </bpmn:userTask>
                <bpmn:endEvent id="endEvent" name="结束">
                  <bpmn:incoming>flow2</bpmn:incoming>
                </bpmn:endEvent>
                <bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="activity1"/>
                <bpmn:sequenceFlow id="flow2" sourceRef="activity1" targetRef="endEvent"/>
              </bpmn:process>
              <bpmndi:BPMNDiagram id="BPMNDiagram_%1$s">
                <bpmndi:BPMNPlane id="BPMNPlane_%1$s" bpmnElement="%1$s">
                  <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
                    <dc:Bounds x="152" y="102" width="36" height="36"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape id="activity1_di" bpmnElement="activity1">
                    <dc:Bounds x="240" y="80" width="100" height="80"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
                    <dc:Bounds x="392" y="102" width="36" height="36"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
                    <di:waypoint x="188" y="120"/>
                    <di:waypoint x="240" y="120"/>
                  </bpmndi:BPMNEdge>
                  <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
                    <di:waypoint x="340" y="120"/>
                    <di:waypoint x="392" y="120"/>
                  </bpmndi:BPMNEdge>
                </bpmndi:BPMNPlane>
              </bpmndi:BPMNDiagram>
            </bpmn:definitions>
            """;

    /**
     * 流程模型分页列表
     *
     * @param queryParams 查询参数
     */
    @Override
    public Page<ModelVO> getModelPage(WorkflowModelPageQuery queryParams) {
        ModelQuery query = repositoryService.createModelQuery();
        if (StrUtil.isNotBlank(queryParams.getKeywords())) {
            query.modelNameLike("%" + queryParams.getKeywords() + "%");
        }
        query.orderByLastUpdateTime().desc();
        long total = query.count();
        List<Model> models = query.listPage((queryParams.getPageNum() - 1) * queryParams.getPageSize(),
                queryParams.getPageSize());

        Page<ModelVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize(), total);
        List<ModelVO> records = models.stream().map(this::toModelVO).toList();
        fillPublishState(records);
        page.setRecords(records);
        return page;
    }

    /**
     * 填充发布状态：模型与流程定义经 key 关联，未发布的模型无定义信息
     */
    private void fillPublishState(List<ModelVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<String, ProcessDefinition> latestDefinitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion().list().stream()
                .collect(Collectors.toMap(ProcessDefinition::getKey, definition -> definition, (a, b) -> a));
        if (latestDefinitions.isEmpty()) {
            return;
        }
        // 部署时间在 ACT_RE_DEPLOYMENT，批量取避免逐行查询
        Map<String, Date> deploymentTimes = latestDefinitions.values().stream()
                .map(ProcessDefinition::getDeploymentId).distinct()
                .map(id -> repositoryService.createDeploymentQuery().deploymentId(id).singleResult())
                .collect(Collectors.toMap(Deployment::getId, Deployment::getDeploymentTime));
        records.forEach(modelVO -> {
            ProcessDefinition definition = latestDefinitions.get(modelVO.getKey());
            if (definition == null) {
                return;
            }
            modelVO.setDefinitionId(definition.getId());
            modelVO.setPublishedVersion(definition.getVersion());
            modelVO.setSuspended(definition.isSuspended());
            modelVO.setDeploymentTime(deploymentTimes.get(definition.getDeploymentId()));
        });
    }

    /**
     * 新增流程模型
     *
     * @param formData 模型表单数据
     */
    @Override
    public boolean saveModel(ModelForm formData) {
        long count = repositoryService.createModelQuery().modelKey(formData.getKey()).count();
        Assert.isTrue(count == 0, "模型标识已存在");

        Model model = repositoryService.newModel();
        model.setKey(formData.getKey());
        model.setName(formData.getName());
        model.setMetaInfo(buildMetaInfo(formData.getDescription()));
        repositoryService.saveModel(model);
        repositoryService.addModelEditorSource(model.getId(),
                INITIAL_XML_TEMPLATE.formatted(formData.getKey(), formData.getName())
                        .getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /**
     * 修改流程模型
     *
     * @param modelId  模型ID
     * @param formData 模型表单数据
     */
    @Override
    public boolean updateModel(String modelId, ModelForm formData) {
        Model model = getModel(modelId);
        model.setName(formData.getName());
        model.setMetaInfo(buildMetaInfo(formData.getDescription()));
        repositoryService.saveModel(model);
        return true;
    }

    /**
     * 删除流程模型
     *
     * @param modelIds 模型ID集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeModels(List<String> modelIds) {
        // 先全量校验再删除：批量删除中途抛错时，前面已删的模型无法回退
        List<Model> models = modelIds.stream()
                .map(repositoryService::getModel)
                .filter(Objects::nonNull)
                .toList();
        models.forEach(this::checkNoRunningInstance);
        models.forEach(this::removeModelAndDeployments);
    }

    /**
     * 校验流程无运行中实例
     */
    private void checkNoRunningInstance(Model model) {
        long runningCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(model.getKey())
                .count();
        if (runningCount > 0) {
            throw new BusinessException("流程【" + model.getName() + "】存在 " + runningCount
                    + " 个运行中的实例，请先撤销或办结后再删除");
        }
    }

    /**
     * 删除模型的全部发布版本（级联流程定义、实例与历史）及设计草稿
     */
    private void removeModelAndDeployments(Model model) {
        repositoryService.createDeploymentQuery()
                .processDefinitionKey(model.getKey()).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
        repositoryService.deleteModel(model.getId());
    }

    /**
     * 查询引用指定表单的流程
     *
     * @param formKeys 表单标识集合
     * @return 表单标识 → 引用它的流程名称集合（无引用返回空 Map）
     */
    @Override
    public Map<String, Set<String>> getFormReferences(Collection<String> formKeys) {
        if (CollUtil.isEmpty(formKeys)) {
            return Map.of();
        }
        Set<String> targets = new HashSet<>(formKeys);
        Map<String, Set<String>> references = new HashMap<>();

        // 已部署定义：全部版本都算引用，旧版本的实例仍在流转
        for (ProcessDefinition definition : repositoryService.createProcessDefinitionQuery().list()) {
            Set<String> referenced;
            try {
                referenced = bpmnModelHelper.getFormKeys(definition.getId());
            } catch (Exception e) {
                log.warn("流程定义 [{}] 表单引用解析失败，跳过校验", definition.getKey());
                continue;
            }
            referenced.stream().filter(targets::contains)
                    .forEach(formKey -> recordReference(references, formKey, definition.getName()));
        }

        // 模型草稿：设计器里已绑定表单，删除后该模型发布即成无表单流程
        for (Model model : repositoryService.createModelQuery().list()) {
            byte[] source = repositoryService.getModelEditorSource(model.getId());
            if (source == null || source.length == 0) {
                continue;
            }
            Set<String> referenced;
            try {
                referenced = bpmnModelHelper.getFormKeysFromXml(new String(source, StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.warn("流程模型 [{}] 表单引用解析失败，跳过校验", model.getKey());
                continue;
            }
            referenced.stream().filter(targets::contains)
                    .forEach(formKey -> recordReference(references, formKey, model.getName()));
        }
        return references;
    }

    /**
     * 记录一条引用关系（同一表单被多个流程引用时累加，按流程名称排序保证提示稳定）
     */
    private void recordReference(Map<String, Set<String>> references, String formKey, String processName) {
        references.computeIfAbsent(formKey, key -> new TreeSet<>()).add(processName);
    }

    /**
     * 获取模型 BPMN XML
     *
     * @param modelId 模型ID
     */
    @Override
    public ModelXmlVO getModelXml(String modelId) {
        getModel(modelId);
        byte[] source = repositoryService.getModelEditorSource(modelId);
        return new ModelXmlVO(source == null ? "" : new String(source, StandardCharsets.UTF_8));
    }

    /**
     * 保存模型 BPMN XML
     *
     * @param modelId  模型ID
     * @param formData XML 表单数据
     */
    @Override
    public boolean saveModelXml(String modelId, ModelXmlForm formData) {
        getModel(modelId);
        repositoryService.addModelEditorSource(modelId, formData.getXml().getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /**
     * 部署流程模型
     *
     * @param modelId 模型ID
     */
    @Override
    public void deployModel(String modelId) {
        Model model = getModel(modelId);
        byte[] source = repositoryService.getModelEditorSource(modelId);
        if (source == null || source.length == 0) {
            throw new BusinessException("模型内容为空，请先设计流程图");
        }
        String bpmnXml = new String(source, StandardCharsets.UTF_8);
        // 无主任务发布前拦截：未配置办理人的节点不会进入任何人待办，流程将永久卡死
        List<String> unassignedTasks = bpmnModelHelper.getUnassignedTaskNames(bpmnXml);
        if (!unassignedTasks.isEmpty()) {
            throw new BusinessException("节点【" + String.join("、", unassignedTasks)
                    + "】未配置办理人，发布后将无人可审批，请补充后重试");
        }
        // 候选组角色存在性校验：指向不存在的角色（拼写错误/已删除）同样产生无人可办的任务
        Set<String> groupCodes = bpmnModelHelper.getCandidateGroups(bpmnXml);
        if (!groupCodes.isEmpty()) {
            Set<String> existingCodes = roleService.list(
                            new LambdaQueryWrapper<Role>().in(Role::getCode, groupCodes))
                    .stream().map(Role::getCode).collect(Collectors.toSet());
            List<String> invalidCodes = groupCodes.stream()
                    .filter(code -> !existingCodes.contains(code)).sorted().toList();
            if (!invalidCodes.isEmpty()) {
                throw new BusinessException("候选组【" + String.join("、", invalidCodes)
                        + "】不存在，请核对角色编码后重试");
            }
        }
        try {
            repositoryService.createDeployment()
                    .name(model.getName())
                    .key(model.getKey())
                    .addString(model.getKey() + ".bpmn20.xml", bpmnXml)
                    .deploy();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 引擎部署时做 BPMN 解析，节点未连线、非法 XML 均在此暴露
            throw new BusinessException("流程文件校验失败，请检查节点连线与属性配置");
        }
    }

    /**
     * 获取模型（不存在即抛业务异常）
     */
    private Model getModel(String modelId) {
        Model model = repositoryService.getModel(modelId);
        Assert.notNull(model, "流程模型不存在");
        return model;
    }

    /**
     * 构建 metaInfo（Flowable 模型约定格式，description 存放于 JSON）
     */
    private String buildMetaInfo(String description) {
        JSONObject metaInfo = JSONUtil.createObj().set("description", description);
        return metaInfo.toString();
    }

    private ModelVO toModelVO(Model model) {
        ModelVO modelVO = new ModelVO();
        modelVO.setId(model.getId());
        modelVO.setName(model.getName());
        modelVO.setKey(model.getKey());
        modelVO.setDescription(parseMetaDescription(model.getMetaInfo()));
        modelVO.setVersion(model.getVersion());
        modelVO.setCreateTime(model.getCreateTime());
        modelVO.setUpdateTime(model.getLastUpdateTime());
        return modelVO;
    }

    private String parseMetaDescription(String metaInfo) {
        if (StrUtil.isBlank(metaInfo) || !JSONUtil.isTypeJSONObject(metaInfo)) {
            return null;
        }
        return JSONUtil.parseObj(metaInfo).getStr("description");
    }

}
