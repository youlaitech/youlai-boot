package com.youlai.boot.platform.codegen.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 数据表查询对象
 *
 * @author Ray
 * @since 2.10.0
 */
@Schema(description = "数据表查询对象")
@Getter
@Setter
public class TableQuery extends BaseQuery {

    @Schema(description="关键字(表名)")
    private String keywords;

    /**
     * 排除的表名
     */
    @JsonIgnore
    private List<String> excludeTables;

}
