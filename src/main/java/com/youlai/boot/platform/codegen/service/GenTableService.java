package com.youlai.boot.platform.codegen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.platform.codegen.model.entity.GenTable;
import com.youlai.boot.platform.codegen.model.form.GenConfigForm;

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

}
