package com.youlai.boot.module.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.module.form.model.entity.FormSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单版本快照访问层
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Mapper
public interface FormSnapshotMapper extends BaseMapper<FormSnapshot> {

}
