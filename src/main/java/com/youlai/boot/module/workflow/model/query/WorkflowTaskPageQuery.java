package com.youlai.boot.module.workflow.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务分页查询对象（待办/已办共用）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "任务分页查询对象")
public class WorkflowTaskPageQuery extends BaseQuery {

    @Schema(description = "关键字（任务名称）")
    private String keywords;

}
