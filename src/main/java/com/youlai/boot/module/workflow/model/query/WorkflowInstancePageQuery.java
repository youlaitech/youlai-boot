package com.youlai.boot.module.workflow.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程实例分页查询对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "流程实例分页查询对象")
public class WorkflowInstancePageQuery extends BaseQuery {

    @Schema(description = "关键字（流程定义名称）")
    private String keywords;

}
