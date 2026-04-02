package com.youlai.boot.codegen.model.vo;

import lombok.Data;

/**
 * 数据表元数据
 *
 * @author Ray
 * @since 2.10.0
 */
@Data
public class TableMetaVO {

    private String tableName;

    private String tableComment;

    private String tableCollation;

    private String engine;

    private String charset;

    private String createTime;

}
