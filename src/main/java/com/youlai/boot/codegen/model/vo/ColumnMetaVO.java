package com.youlai.boot.codegen.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "数据表字段元数据")
@Data
public class ColumnMetaVO {

    private String columnName;

    private String dataType;

    private String columnComment;

    private Long characterMaximumLength;

    private Integer isPrimaryKey;

    private String isNullable;

    private String characterSetName;

    private String collationName;

}
