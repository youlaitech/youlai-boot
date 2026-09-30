package com.youlai.boot.codegen.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.YouLaiBootApplication;
import com.youlai.boot.common.enums.EnvEnum;
import com.youlai.boot.codegen.enums.FormTypeEnum;
import com.youlai.boot.codegen.enums.JavaTypeEnum;
import com.youlai.boot.codegen.enums.QueryTypeEnum;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.codegen.property.CodegenProperties;
import com.youlai.boot.codegen.converter.CodegenConverter;
import com.youlai.boot.codegen.mapper.DatabaseMapper;
import com.youlai.boot.codegen.mapper.GenTableMapper;
import com.youlai.boot.codegen.model.vo.ColumnMetaVO;
import com.youlai.boot.codegen.model.vo.TableMetaVO;
import com.youlai.boot.codegen.model.entity.GenTable;
import com.youlai.boot.codegen.model.entity.GenTableColumn;
import com.youlai.boot.codegen.model.form.GenConfigForm;
import com.youlai.boot.codegen.model.vo.GenConfigAiVO;
import com.youlai.boot.codegen.service.GenTableService;
import com.youlai.boot.codegen.service.GenTableColumnService;
import com.youlai.boot.support.ai.AiChatService;
import com.youlai.boot.support.ai.PromptLoader;
import com.youlai.boot.system.model.dto.MenuCreateDTO;
import com.youlai.boot.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库服务实现类
 *
 * @author Ray
 * @since 2.10.0
 */
@Service
@RequiredArgsConstructor
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements GenTableService {

    // 系统提示词名称，对应 resources/prompts 下的相对路径
    private static final String SYSTEM_PROMPT_PATH = "codegen/system.md";

    private final DatabaseMapper databaseMapper;
    private final CodegenProperties codegenProperties;
    private final GenTableColumnService genTableColumnService;
    private final CodegenConverter codegenConverter;
    private final AiChatService aiChatService;
    private final PromptLoader promptLoader;

    @Value("${spring.profiles.active}")
    private String springProfilesActive;

    private final MenuService menuService;

    /**
     * 获取代码生成配置
     *
     * @param tableName 表名 eg: sys_user
     * @return 代码生成配置
     */
    @Override
    public GenConfigForm getGenTableFormData(String tableName) {
        // 查询表生成配置
        GenTable genTable = this.getOne(
                new LambdaQueryWrapper<>(GenTable.class)
                        .eq(GenTable::getTableName, tableName)
                        .last("LIMIT 1")
        );

        // 是否有代码生成配置
        boolean hasGenTable = genTable != null;

        // 如果没有代码生成配置，则根据表的元数据生成默认配置
        if (genTable == null) {
            TableMetaVO tableMetadata = databaseMapper.getTableMetadata(tableName);
            Assert.isTrue(tableMetadata != null, "未找到表元数据");

            genTable = new GenTable();
            genTable.setTableName(tableName);

            // 表注释作为业务名称，去掉表字 例如：用户表 -> 用户
            String tableComment = tableMetadata.getTableComment();
            if (StrUtil.isNotBlank(tableComment)) {
                genTable.setBusinessName(tableComment.replace("表", "").trim());
            }
            //  根据表名生成实体类名，支持去除前缀 例如：sys_user -> SysUser
            String removePrefix = genTable.getRemoveTablePrefix();
            String processedTable = tableName;
            if (StrUtil.isNotBlank(removePrefix) && StrUtil.startWith(tableName, removePrefix)) {
                processedTable = StrUtil.removePrefix(tableName, removePrefix);
            }
            genTable.setEntityName(StrUtil.toCamelCase(StrUtil.upperFirst(StrUtil.toCamelCase(processedTable))));

            genTable.setPackageName(YouLaiBootApplication.class.getPackageName());
            genTable.setModuleName(codegenProperties.getDefaultConfig().getModuleName()); // 默认模块名
            genTable.setAuthor(codegenProperties.getDefaultConfig().getAuthor());
        }

        // 根据表的列 + 已经存在的字段生成配置 得到 组合后的字段生成配置
        List<GenTableColumn> genTableColumns = new ArrayList<>();

        // 获取表的列
        List<ColumnMetaVO> tableColumns = databaseMapper.getTableColumns(tableName);
        if (CollectionUtil.isNotEmpty(tableColumns)) {
            // 查询字段生成配置
            List<GenTableColumn> fieldConfigList = genTableColumnService.list(
                    new LambdaQueryWrapper<GenTableColumn>()
                            .eq(GenTableColumn::getTableId, genTable.getId())
                            .orderByAsc(GenTableColumn::getFieldSort)
            );
            Integer maxSort = fieldConfigList.stream()
                    .map(GenTableColumn::getFieldSort)
                    .filter(Objects::nonNull) // 过滤掉空值
                    .max(Integer::compareTo)
                    .orElse(0);
            for (ColumnMetaVO tableColumn : tableColumns) {
                // 根据列名获取字段生成配置
                String columnName = tableColumn.getColumnName();
                GenTableColumn fieldConfig = fieldConfigList.stream()
                        .filter(item -> StrUtil.equals(item.getColumnName(), columnName))
                        .findFirst()
                        .orElseGet(() -> createDefaultFieldConfig(tableColumn));
                if (fieldConfig.getFieldSort() == null) {
                    fieldConfig.setFieldSort(++maxSort);
                }
                // 根据列类型设置字段类型
                String fieldType = fieldConfig.getFieldType();
                if (StrUtil.isBlank(fieldType)) {
                    String javaType = JavaTypeEnum.getJavaTypeByColumnType(fieldConfig.getColumnType());
                    fieldConfig.setFieldType(javaType);
                }
                // 如果没有代码生成配置，则默认展示在列表和表单
                if (!hasGenTable) {
                    fieldConfig.setIsShowInList(1);
                    fieldConfig.setIsShowInForm(1);
                }
                genTableColumns.add(fieldConfig);
            }
        }
        // 对 genTableColumns 按照 fieldSort 排序
        genTableColumns = genTableColumns.stream().sorted(Comparator.comparing(GenTableColumn::getFieldSort)).toList();
        GenConfigForm genConfigForm = codegenConverter.toGenConfigForm(genTable, genTableColumns);

        genConfigForm.setFrontendAppName(codegenProperties.getFrontendAppName());
        genConfigForm.setBackendAppName(codegenProperties.getBackendAppName());
        return genConfigForm;
    }


    /**
     * 创建默认字段配置
     *
     * @param columnMetaVO 表字段元数据
     * @return
     */
    private GenTableColumn createDefaultFieldConfig(ColumnMetaVO columnMetaVO) {
        GenTableColumn fieldConfig = new GenTableColumn();
        fieldConfig.setColumnName(columnMetaVO.getColumnName());
        fieldConfig.setColumnType(columnMetaVO.getDataType());
        fieldConfig.setFieldComment(columnMetaVO.getColumnComment());
        fieldConfig.setFieldName(StrUtil.toCamelCase(columnMetaVO.getColumnName()));
        fieldConfig.setIsRequired("YES".equals(columnMetaVO.getIsNullable()) ? 0 : 1);

        String columnType = StrUtil.blankToDefault(fieldConfig.getColumnType(), "").toLowerCase();
        if ("date".equals(columnType)) {
            fieldConfig.setFormType(FormTypeEnum.DATE);
        } else if ("datetime".equals(columnType) || "timestamp".equals(columnType)) {
            fieldConfig.setFormType(FormTypeEnum.DATE_TIME);
        } else {
            fieldConfig.setFormType(FormTypeEnum.INPUT);
        }

        fieldConfig.setQueryType(QueryTypeEnum.EQ);
        fieldConfig.setMaxLength(columnMetaVO.getCharacterMaximumLength());
        return fieldConfig;
    }

    /**
     * 保存代码生成配置
     *
     * @param formData 代码生成配置表单
     */
    @Override
    public void saveGenConfig(GenConfigForm formData) {
        GenTable genTable = codegenConverter.toGenTable(formData);
        this.saveOrUpdate(genTable);

        // 如果选择上级菜单且当前环境不是生产环境，则保存菜单
        Long parentMenuId = formData.getParentMenuId();
        if (parentMenuId != null && !EnvEnum.PROD.getValue().equals(springProfilesActive)) {
            MenuCreateDTO dto = new MenuCreateDTO(
                    genTable.getBusinessName(),
                    genTable.getEntityName(),
                    genTable.getModuleName(),
                    genTable.getTableName()
            );
            menuService.addMenuForCodegen(parentMenuId, dto);
        }

        List<GenTableColumn> genTableColumns = codegenConverter.toGenTableColumn(formData.getFieldConfigs());

        if (CollectionUtil.isEmpty(genTableColumns)) {
            throw new BusinessException("字段配置不能为空");
        }
        genTableColumns.forEach(genTableColumn -> {
            genTableColumn.setTableId(genTable.getId());
        });
        genTableColumnService.saveOrUpdateBatch(genTableColumns);
    }

    /**
     * 删除代码生成配置
     *
     * @param tableName 表名
     */
    @Override
    public void deleteGenConfig(String tableName) {
        GenTable genTable = this.getOne(new LambdaQueryWrapper<GenTable>()
                .eq(GenTable::getTableName, tableName));

        boolean result = this.remove(new LambdaQueryWrapper<GenTable>()
                .eq(GenTable::getTableName, tableName)
        );
        if (result) {
            genTableColumnService.remove(new LambdaQueryWrapper<GenTableColumn>()
                    .eq(GenTableColumn::getTableId, genTable.getId())
            );
        }
    }

    //---------------------------------------------------
    // AI 辅助：未开启 AI 时调用返回业务异常
    //---------------------------------------------------

    /**
     * 用 AI 推断代码生成配置
     *
     * @param tableName   表名 eg: sys_user
     * @param requirement 补充需求描述，可为空
     * @return 回填了 AI 推断结果的配置
     */
    @Override
    public GenConfigForm aiFillConfig(String tableName, String requirement) {
        GenConfigForm config = getGenTableFormData(tableName);
        String systemPrompt = promptLoader.render(SYSTEM_PROMPT_PATH, buildAiPromptVariables());
        GenConfigAiVO aiConfig = aiChatService.chatEntity(systemPrompt, buildAiUserPrompt(config, requirement), GenConfigAiVO.class);

        if (StrUtil.isNotBlank(aiConfig.getBusinessName())) {
            config.setBusinessName(aiConfig.getBusinessName().trim());
        }
        applyAiFieldConfigs(config, aiConfig.getFieldConfigs());
        return config;
    }

    /**
     * 构造系统提示词的变量值，表单类型与查询类型清单取自枚举，新增枚举值无需改动提示词
     *
     * @return 变量值
     */
    private Map<String, String> buildAiPromptVariables() {
        String formTypes = Arrays.stream(FormTypeEnum.values())
                .map(type -> type.name() + "(" + type.getLabel() + ")")
                .collect(Collectors.joining("|"));
        String queryTypes = Arrays.stream(QueryTypeEnum.values())
                .map(Enum::name)
                .collect(Collectors.joining("|"));
        return Map.of("formTypes", formTypes, "queryTypes", queryTypes);
    }

    /**
     * 按列名匹配回填字段配置
     *
     * @param config   原配置
     * @param aiFields AI 返回的字段列表
     */
    private void applyAiFieldConfigs(GenConfigForm config, List<GenConfigAiVO.FieldAiVO> aiFields) {
        if (CollectionUtil.isEmpty(aiFields) || CollectionUtil.isEmpty(config.getFieldConfigs())) {
            return;
        }
        Map<String, GenConfigAiVO.FieldAiVO> aiFieldMap = aiFields.stream()
                .filter(field -> StrUtil.isNotBlank(field.getColumnName()))
                .collect(Collectors.toMap(GenConfigAiVO.FieldAiVO::getColumnName, Function.identity(),
                        (existing, replacement) -> existing, LinkedHashMap::new));

        config.getFieldConfigs().forEach(field -> {
            GenConfigAiVO.FieldAiVO aiField = aiFieldMap.get(field.getColumnName());
            if (aiField == null) {
                return;
            }
            if (StrUtil.isNotBlank(aiField.getFieldComment())) {
                field.setFieldComment(aiField.getFieldComment().trim());
            }
            if (StrUtil.isNotBlank(aiField.getDictType())) {
                field.setDictType(aiField.getDictType().trim());
            }
            field.setFormType(parseAiEnum(aiField.getFormType(), FormTypeEnum::valueOf, field.getFormType()));
            field.setQueryType(parseAiEnum(aiField.getQueryType(), QueryTypeEnum::valueOf, field.getQueryType()));
            field.setIsRequired(resolveAiValue(aiField.getIsRequired(), field.getIsRequired()));
            field.setIsShowInList(resolveAiValue(aiField.getIsShowInList(), field.getIsShowInList()));
            field.setIsShowInForm(resolveAiValue(aiField.getIsShowInForm(), field.getIsShowInForm()));
            field.setIsShowInQuery(resolveAiValue(aiField.getIsShowInQuery(), field.getIsShowInQuery()));
        });
    }

    /**
     * 解析枚举名，非法或未给出时沿用原值
     *
     * @param name     枚举名
     * @param parser   枚举解析器
     * @param fallback 原值
     * @return 枚举或原值
     */
    private <T> T parseAiEnum(String name, Function<String, T> parser, T fallback) {
        if (StrUtil.isBlank(name)) {
            return fallback;
        }
        try {
            return parser.apply(name.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * AI 值缺失时沿用原值
     *
     * @param aiValue  AI 值
     * @param fallback 原值
     * @return 生效值
     */
    private <T> T resolveAiValue(T aiValue, T fallback) {
        return aiValue != null ? aiValue : fallback;
    }

    /**
     * 构造用户提示词
     *
     * @param config      当前配置
     * @param requirement 补充需求描述
     * @return 用户提示词
     */
    private String buildAiUserPrompt(GenConfigForm config, String requirement) {
        List<Map<String, Object>> columns = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(config.getFieldConfigs())) {
            config.getFieldConfigs().forEach(field -> {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("columnName", field.getColumnName());
                column.put("columnType", field.getColumnType());
                column.put("fieldComment", field.getFieldComment());
                column.put("isRequired", field.getIsRequired());
                columns.add(column);
            });
        }
        return """
                表名：%s
                当前业务名：%s
                补充说明：%s
                字段列表：
                %s
                """.formatted(config.getTableName(), config.getBusinessName(),
                StrUtil.blankToDefault(requirement, "无"), JSONUtil.toJsonStr(columns));
    }

}
