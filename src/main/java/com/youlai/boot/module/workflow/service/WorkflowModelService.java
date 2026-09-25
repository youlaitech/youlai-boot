package com.youlai.boot.module.workflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.workflow.model.form.ModelForm;
import com.youlai.boot.module.workflow.model.form.ModelXmlForm;
import com.youlai.boot.module.workflow.model.query.WorkflowModelPageQuery;
import com.youlai.boot.module.workflow.model.vo.ModelVO;
import com.youlai.boot.module.workflow.model.vo.ModelXmlVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程模型业务接口（设计器侧：模型 CRUD、XML 保存、部署、表单引用查询）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface WorkflowModelService {

    /**
     * 流程模型分页列表
     *
     * @param queryParams 查询参数
     * @return 分页结果
     */
    Page<ModelVO> getModelPage(WorkflowModelPageQuery queryParams);

    /**
     * 新增流程模型（内置开始-审批-结束模板，设计器开箱即画）
     *
     * @param formData 模型表单数据
     * @return 是否成功
     */
    boolean saveModel(ModelForm formData);

    /**
     * 修改流程模型（标识创建后不可修改）
     *
     * @param modelId  模型ID
     * @param formData 模型表单数据
     * @return 是否成功
     */
    boolean updateModel(String modelId, ModelForm formData);

    /**
     * 删除流程模型（级联删除其全部发布版本；存在运行中实例时拦截）
     *
     * @param modelIds 模型ID集合
     */
    void removeModels(List<String> modelIds);

    /**
     * 查询引用指定表单的流程（已部署定义与模型草稿的表单绑定）
     *
     * @param formKeys 表单标识集合
     * @return 表单标识 → 引用它的流程名称集合（无引用返回空 Map）
     */
    Map<String, Set<String>> getFormReferences(Collection<String> formKeys);

    /**
     * 获取模型 BPMN XML（设计器回显）
     *
     * @param modelId 模型ID
     * @return BPMN 2.0 XML
     */
    ModelXmlVO getModelXml(String modelId);

    /**
     * 保存模型 BPMN XML（设计器产出）
     *
     * @param modelId  模型ID
     * @param formData XML 表单数据
     * @return 是否成功
     */
    boolean saveModelXml(String modelId, ModelXmlForm formData);

    /**
     * 部署流程模型（校验 XML 并发布为流程定义新版本）
     *
     * @param modelId 模型ID
     */
    void deployModel(String modelId);

}
