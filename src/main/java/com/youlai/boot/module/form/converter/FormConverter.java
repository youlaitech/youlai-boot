package com.youlai.boot.module.form.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.form.model.entity.FormDefinition;
import com.youlai.boot.module.form.model.form.FormDefinitionForm;
import com.youlai.boot.module.form.model.vo.FormDefinitionPageVO;
import org.mapstruct.Mapper;

/**
 * 表单定义 对象转换器
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Mapper(componentModel = "spring")
public interface FormConverter {

    Page<FormDefinitionPageVO> toPageVo(Page<FormDefinition> page);

    FormDefinitionForm toForm(FormDefinition entity);

    FormDefinition toEntity(FormDefinitionForm form);

}
