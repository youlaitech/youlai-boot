package com.youlai.boot.module.form.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单数据查询对象
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "表单数据查询对象")
public class FormDataQuery extends BaseQuery {

}
