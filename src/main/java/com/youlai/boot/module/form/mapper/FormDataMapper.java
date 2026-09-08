package com.youlai.boot.module.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.module.form.model.entity.FormData;
import com.youlai.boot.module.form.model.query.FormDataPageQuery;
import com.youlai.boot.module.form.model.vo.FormDataVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单数据 访问层
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Mapper
public interface FormDataMapper extends BaseMapper<FormData> {

    /**
     * 表单数据分页列表（联查提交人昵称）
     *
     * @param page        分页参数
     * @param formId      表单定义ID
     * @param queryParams 查询参数
     * @return 表单数据分页列表
     */
    Page<FormDataVO> getFormDataPage(Page<FormDataVO> page, Long formId, FormDataPageQuery queryParams);

}
