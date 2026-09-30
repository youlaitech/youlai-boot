package com.youlai.boot.module.form.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.form.model.query.FormDataPageQuery;
import com.youlai.boot.module.form.model.vo.FormDataVO;

import java.util.List;
import java.util.Map;

/**
 * 表单数据业务接口
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public interface FormDataService {

    /**
     * 提交表单数据（仅已发布表单可提交，提交时快照表单版本）
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     * @return 是否提交成功
     */
    boolean saveFormData(String formKey, Map<String, Object> data);

    /**
     * 提交表单数据并返回数据ID
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     * @return 表单数据ID
     */
    Long saveFormDataReturnId(String formKey, Map<String, Object> data);

    /**
     * 匿名提交公开表单数据（公开分享链接入口）
     *
     * @param formKey 表单唯一标识
     * @param data    表单数据（field -> value 映射）
     * @param ip      提交方 IP（限流维度）
     * @return 是否提交成功
     */
    boolean savePublicFormData(String formKey, Map<String, Object> data, String ip);

    /**
     * 表单数据分页列表
     *
     * @param formKey     表单唯一标识
     * @param queryParams 查询参数
     * @return 表单数据分页列表
     */
    Page<FormDataVO> getFormDataPage(String formKey, FormDataPageQuery queryParams);

    /**
     * 获取表单数据详情（只读回显）
     *
     * @param dataId 数据ID
     * @return 表单数据详情
     */
    FormDataVO getFormDataDetail(Long dataId);

    /**
     * 按表单标识删除全部提交数据（演示数据重置用）
     *
     * @param formKey 表单唯一标识
     */
    void removeByFormKey(String formKey);

    /**
     * 批量删除表单数据
     *
     * @param ids 数据ID列表
     */
    void removeFormData(List<Long> ids);

}
