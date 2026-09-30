package com.youlai.boot.codegen.model.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 推断的代码生成配置
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Data
public class GenConfigAiVO {

    // 业务名
    private String businessName;

    // 字段配置列表
    private List<FieldAiVO> fieldConfigs;

    /**
     * AI 推断的字段配置
     */
    @Data
    public static class FieldAiVO {

        // 列名，须与入参一致
        private String columnName;

        // 字段描述
        private String fieldComment;

        // 表单类型枚举名
        private String formType;

        // 查询类型枚举名
        private String queryType;

        // 是否必填
        private Integer isRequired;

        // 是否在列表显示
        private Integer isShowInList;

        // 是否在表单显示
        private Integer isShowInForm;

        // 是否在查询条件显示
        private Integer isShowInQuery;

        // 字典类型编码
        private String dictType;
    }
}
