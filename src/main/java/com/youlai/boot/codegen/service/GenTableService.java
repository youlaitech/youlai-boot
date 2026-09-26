package com.youlai.boot.codegen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.codegen.model.entity.GenTable;
import com.youlai.boot.codegen.model.form.GenConfigForm;

/**
 * 代码生成配置接口
 *
 * @author Ray
 * @since 2.10.0
 */
public interface GenTableService extends IService<GenTable> {

    /**
     * 获取代码生成配置
     *
     * @param tableName 表名
     * @return
     */
    GenConfigForm getGenTableFormData(String tableName);

    /**
     * 保存代码生成配置
     *
     * @param formData 表单数据
     * @return
     */
    void saveGenConfig(GenConfigForm formData);

    /**
     * 删除代码生成配置
     *
     * @param tableName 表名
     * @return
     */
    void deleteGenConfig(String tableName);

    /**
     * 用 AI 推断代码生成配置，未推断出的字段沿用原有默认值
     *
     * @param tableName   表名
     * @param requirement 补充需求描述，可为空
     * @return 回填了 AI 推断结果的配置
     */
    GenConfigForm aiFillConfig(String tableName, String requirement);

}
