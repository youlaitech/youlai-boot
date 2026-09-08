package com.youlai.boot.module.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.query.FormDefinitionPageQuery;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单定义 访问层
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Mapper
public interface FormDefinitionMapper extends BaseMapper<FormDefinition> {

    /**
     * 表单定义分页列表
     *
     * @param page        分页参数
     * @param queryParams 查询参数
     * @return 表单定义分页列表
     */
    Page<FormDefinitionPageVO> getFormDefinitionPage(Page<FormDefinitionPageVO> page, FormDefinitionPageQuery queryParams);

}
