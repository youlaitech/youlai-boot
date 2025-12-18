package com.youlai.boot.platform.codegen.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.YouLaiBootApplication;
import com.youlai.boot.common.enums.EnvEnum;
import com.youlai.boot.platform.codegen.enums.FormTypeEnum;
import com.youlai.boot.platform.codegen.enums.JavaTypeEnum;
import com.youlai.boot.platform.codegen.enums.QueryTypeEnum;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.config.property.CodegenProperties;
import com.youlai.boot.platform.codegen.converter.CodegenConverter;
import com.youlai.boot.platform.codegen.mapper.DatabaseMapper;
import com.youlai.boot.platform.codegen.mapper.GenTableMapper;
import com.youlai.boot.platform.codegen.model.bo.ColumnMetaData;
import com.youlai.boot.platform.codegen.model.bo.TableMetaData;
import com.youlai.boot.platform.codegen.model.entity.GenTable;
import com.youlai.boot.platform.codegen.model.entity.GenTableColumn;
import com.youlai.boot.platform.codegen.model.form.GenConfigForm;
import com.youlai.boot.platform.codegen.service.GenTableService;
import com.youlai.boot.platform.codegen.service.GenTableColumnService;
import com.youlai.boot.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 数据库服务实现类
 *
 * @author Ray
 * @since 2.10.0
 */
@Service
@RequiredArgsConstructor
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements GenTableService {

    private final DatabaseMapper databaseMapper;
    private final CodegenProperties codegenProperties;
    private final GenTableColumnService genTableColumnService;
    private final CodegenConverter codegenConverter;

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
            TableMetaData tableMetadata = databaseMapper.getTableMetadata(tableName);
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
        List<ColumnMetaData> tableColumns = databaseMapper.getTableColumns(tableName);
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
            for (ColumnMetaData tableColumn : tableColumns) {
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
     * @param columnMetaData 表字段元数据
     * @return
     */
    private GenTableColumn createDefaultFieldConfig(ColumnMetaData columnMetaData) {
        GenTableColumn fieldConfig = new GenTableColumn();
        fieldConfig.setColumnName(columnMetaData.getColumnName());
        fieldConfig.setColumnType(columnMetaData.getDataType());
        fieldConfig.setFieldComment(columnMetaData.getColumnComment());
        fieldConfig.setFieldName(StrUtil.toCamelCase(columnMetaData.getColumnName()));
        fieldConfig.setIsRequired("YES".equals(columnMetaData.getIsNullable()) ? 0 : 1);

        String columnType = StrUtil.blankToDefault(fieldConfig.getColumnType(), "").toLowerCase();
        if ("date".equals(columnType)) {
            fieldConfig.setFormType(FormTypeEnum.DATE);
        } else if ("datetime".equals(columnType) || "timestamp".equals(columnType)) {
            fieldConfig.setFormType(FormTypeEnum.DATE_TIME);
        } else {
            fieldConfig.setFormType(FormTypeEnum.INPUT);
        }

        fieldConfig.setQueryType(QueryTypeEnum.EQ);
        fieldConfig.setMaxLength(columnMetaData.getCharacterMaximumLength());
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
            menuService.addMenuForCodegen(parentMenuId, genTable);
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



}
