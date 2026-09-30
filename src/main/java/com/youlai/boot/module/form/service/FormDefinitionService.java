package com.youlai.boot.module.form.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.common.model.Option;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.module.form.model.form.FormMenuForm;
import com.youlai.boot.module.form.model.query.FormDefinitionPageQuery;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import com.youlai.boot.module.form.model.vo.FormMenuVO;
import com.youlai.boot.module.form.model.vo.FormRenderVO;

import java.util.List;

/**
 * 表单定义业务接口
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface FormDefinitionService {

    /**
     * 表单定义分页列表
     *
     * @param queryParams 查询参数
     * @return 表单定义分页列表
     */
    Page<FormDefinitionPageVO> getFormDefinitionPage(FormDefinitionPageQuery queryParams);

    /**
     * 按主键查询表单定义（供模块内回退取规则等场景）
     *
     * @param id 表单ID
     * @return 表单定义实体；不存在返回 null
     */
    FormDefinition getFormDefinition(Long id);

    /**
     * 获取表单定义表单数据（编辑回显）
     *
     * @param id 表单ID
     * @return 表单定义表单数据
     */
    FormDefinitionForm getFormDefinitionForm(Long id);

    /**
     * 新增表单定义（初始为草稿态）
     *
     * @param formData 表单定义表单数据
     * @return 是否新增成功
     */
    boolean saveFormDefinition(FormDefinitionForm formData);

    /**
     * 修改表单定义
     *
     * @param id       表单ID
     * @param formData 表单定义表单数据
     * @return 是否修改成功
     */
    boolean updateFormDefinition(Long id, FormDefinitionForm formData);

    /**
     * 批量删除表单定义
     *
     * @param ids 表单ID列表
     */
    void removeFormDefinition(List<Long> ids);

    /**
     * 发布表单（版本号+1，固化版本快照，刷新渲染缓存）
     *
     * @param id 表单ID
     * @return 是否发布成功
     */
    boolean publishFormDefinition(Long id);

    /**
     * 停用表单（清渲染缓存，已发出去的入口立即失效）
     *
     * @param id 表单ID
     * @return 是否停用成功
     */
    boolean disableFormDefinition(Long id);

    /**
     * 按 formKey 获取已发布表单的渲染规则（Redis 缓存）
     *
     * @param formKey 表单唯一标识
     * @return 表单渲染数据
     */
    FormRenderVO getFormRender(String formKey);

    /**
     * 按 formKey 获取公开表单的渲染规则（匿名接口）
     *
     * @param formKey 表单唯一标识
     * @return 表单渲染数据
     */
    FormRenderVO getPublicFormRender(String formKey);

    /**
     * 按 formKey 查询表单定义（任意状态：已停用表单的历史数据仍可查看）
     *
     * @param formKey 表单唯一标识
     * @return 表单定义实体
     */
    FormDefinition getByFormKey(String formKey);

    /**
     * 生成表单访问菜单并授权可见角色（幂等：已生成时更新原菜单）
     *
     * @param formId   表单ID
     * @param menuForm 菜单发布参数
     * @return 是否生成成功
     */
    boolean saveFormMenu(Long formId, FormMenuForm menuForm);

    /**
     * 获取表单已生成的菜单配置（发布向导回显）
     *
     * @param formId 表单ID
     * @return 菜单配置；未生成过返回 null
     */
    FormMenuVO getFormMenu(Long formId);

    /**
     * 审批表单下拉选项（供工作流设计器绑定表单）
     *
     * @return 审批表单下拉选项列表
     */
    List<Option<String>> listWorkflowFormOptions();

    /**
     * 用 AI 把需求描述转成 form-create 规则，产出经结构校验后才返回
     *
     * @param description 需求描述
     * @return 通过结构校验的 form-create rule JSON 字符串
     */
    String aiGenerateRule(String description);

}
