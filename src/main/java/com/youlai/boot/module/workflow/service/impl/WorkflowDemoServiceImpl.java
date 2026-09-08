package com.youlai.boot.module.workflow.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.module.form.service.FormDataService;
import com.youlai.boot.module.workflow.service.WorkflowDemoService;
import com.youlai.boot.module.workflow.service.WorkflowDemoService;
import com.youlai.boot.module.workflow.util.BpmnModelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 演示流程服务实现（请假、报销、采购、用章）
 *
 * <p>初始化按 key 幂等双写：部署流程定义（可发起）+ 创建模型草稿（流程模型页可见），两者各自查重互不依赖。
 * 引擎表（ACT_*）不走 SQL 预置：流程定义跨三表且主键由引擎生成，直接写库绕过解析校验，重置同理依赖引擎级联删除</p>
 *
 * <p>重置为工作流数据全量恢复出厂：清空所有流程定义、模型、实例、历史（含用户自建）及关联表单提交数据后重建演示流程，
 * 供演示环境定期复原，等价于业务脚本"DROP 重建"的数据效果而不动引擎表结构</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDemoServiceImpl implements WorkflowDemoService {

    private final RepositoryService repositoryService;
    private final FormDataService formDataService;
    private final BpmnModelHelper bpmnModelHelper;

    /**
     * 演示流程清单（key 与 resources/workflow/ 下同名 BPMN 文件的 process id 一致）
     */
    private static final List<DemoProcess> DEMO_PROCESSES = List.of(
            new DemoProcess("leave", "请假审批"),
            new DemoProcess("expense", "报销审批"),
            new DemoProcess("purchase", "采购申请"),
            new DemoProcess("seal", "用章申请")
    );

    @Override
    public void initIfAbsent() {
        DEMO_PROCESSES.forEach(this::initDemoProcess);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset() {
        // 删除前收集全部流程定义的发起表单 key：引擎级联不覆盖业务库表单数据，删除后 businessKey 无从追溯
        Set<String> formKeys = collectStartFormKeys();
        // 全量清空：定义（级联删除实例与历史，含用户自建流程）→ 模型草稿 → 流程关联的表单提交数据
        repositoryService.createDeploymentQuery().list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
        repositoryService.createModelQuery().list()
                .forEach(model -> repositoryService.deleteModel(model.getId()));
        formKeys.forEach(this::deleteFormData);
        // 重建演示流程（失败抛异常中断，整体回滚保证重置原子性）
        DEMO_PROCESSES.forEach(this::redeployDemoProcess);
        log.info("工作流数据已重置：清理 {} 个表单的提交数据，重建演示流程 {} 个", formKeys.size(), DEMO_PROCESSES.size());
    }

    /**
     * 收集全部流程定义的发起表单 key（单个定义解析失败不影响其余收集）
     */
    private Set<String> collectStartFormKeys() {
        Set<String> formKeys = new HashSet<>();
        for (ProcessDefinition definition : repositoryService.createProcessDefinitionQuery().list()) {
            try {
                String formKey = bpmnModelHelper.getStartFormKey(definition.getId());
                if (StrUtil.isNotBlank(formKey)) {
                    formKeys.add(formKey);
                }
            } catch (Exception e) {
                log.warn("流程定义 [{}] 表单标识解析失败，跳过其表单数据清理", definition.getKey());
            }
        }
        return formKeys;
    }

    /**
     * 重建单个演示流程（全量清空后调用）
     */
    private void redeployDemoProcess(DemoProcess process) {
        try {
            String bpmnXml = readBpmnXml(process.key());
            repositoryService.createDeployment()
                    .name(process.name())
                    .key(process.key())
                    .addString(process.key() + ".bpmn20.xml", bpmnXml)
                    .deploy();
            createModelIfAbsent(process, bpmnXml);
        } catch (Exception e) {
            throw new BusinessException("演示流程 [" + process.name() + "] 重建失败：" + e.getMessage());
        }
    }

    /**
     * 初始化单个演示流程（资源文件与 key 同名，放 workflow 目录避开 processes/ 自动部署）
     */
    private void initDemoProcess(DemoProcess process) {
        try {
            String bpmnXml = readBpmnXml(process.key());
            deployDefinitionIfAbsent(process, bpmnXml);
            createModelIfAbsent(process, bpmnXml);
        } catch (Exception e) {
            log.error("演示流程 [{}] 初始化失败", process.name(), e);
        }
    }

    /**
     * 幂等部署流程定义（已存在同 key 定义则跳过，删除定义后重启可重新部署）
     */
    private void deployDefinitionIfAbsent(DemoProcess process, String bpmnXml) {
        long count = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(process.key()).count();
        if (count > 0) {
            return;
        }
        repositoryService.createDeployment()
                .name(process.name())
                .key(process.key())
                .addString(process.key() + ".bpmn20.xml", bpmnXml)
                .deploy();
        log.info("演示流程 [{}] 部署完成", process.name());
    }

    /**
     * 幂等创建模型草稿（流程模型页数据源），editorSource 与部署 XML 同源，metaInfo 与服务层约定一致
     */
    private void createModelIfAbsent(DemoProcess process, String bpmnXml) {
        long count = repositoryService.createModelQuery()
                .modelKey(process.key()).count();
        if (count > 0) {
            return;
        }
        Model model = repositoryService.newModel();
        model.setKey(process.key());
        model.setName(process.name());
        model.setMetaInfo(JSONUtil.createObj().set("description", "演示流程").toString());
        repositoryService.saveModel(model);
        repositoryService.addModelEditorSource(model.getId(), bpmnXml.getBytes(StandardCharsets.UTF_8));
        log.info("演示流程 [{}] 模型创建完成", process.name());
    }

    /**
     * 清理演示表单提交数据（流程 businessKey 指向 form_data，随流程一并复位）
     *
     * <p>formKey 到删除条件的解析封装在 form 模块（removeByFormKey 窄接口），本模块不感知表单实体</p>
     */
    private void deleteFormData(String formKey) {
        formDataService.removeByFormKey(formKey);
    }

    /**
     * 读取演示流程 BPMN 资源
     */
    private String readBpmnXml(String key) throws Exception {
        return FileCopyUtils.copyToString(new InputStreamReader(
                new ClassPathResource("workflow/" + key + ".bpmn20.xml").getInputStream(),
                StandardCharsets.UTF_8));
    }

    /**
     * 演示流程定义（key 即 BPMN 文件名与流程标识）
     */
    private record DemoProcess(String key, String name) {
    }

}
