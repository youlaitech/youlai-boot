package com.youlai.boot.platform.codegen.converter;

import com.youlai.boot.platform.codegen.model.entity.GenTable;
import com.youlai.boot.platform.codegen.model.entity.GenTableColumn;
import com.youlai.boot.platform.codegen.model.form.GenConfigForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 代码生成配置转换器
 *
 * @author Ray
 * @since 2.10.0
 */
@Mapper(componentModel = "spring")
public interface CodegenConverter {

    @Mapping(source = "genTable.tableName", target = "tableName")
    @Mapping(source = "genTable.businessName", target = "businessName")
    @Mapping(source = "genTable.moduleName", target = "moduleName")
    @Mapping(source = "genTable.packageName", target = "packageName")
    @Mapping(source = "genTable.entityName", target = "entityName")
    @Mapping(source = "genTable.author", target = "author")
    @Mapping(source = "genTable.pageType", target = "pageType")
    @Mapping(source = "genTable.removeTablePrefix", target = "removeTablePrefix")
    @Mapping(source = "fieldConfigs", target = "fieldConfigs")
    GenConfigForm toGenConfigForm(GenTable genTable, List<GenTableColumn> fieldConfigs);

    List<GenConfigForm.FieldConfig> toGenTableColumnForm(List<GenTableColumn> fieldConfigs);

    GenConfigForm.FieldConfig toGenTableColumnForm(GenTableColumn genTableColumn);

    GenTable toGenTable(GenConfigForm formData);

    List<GenTableColumn> toGenTableColumn(List<GenConfigForm.FieldConfig> fieldConfigs);

    GenTableColumn toGenTableColumn(GenConfigForm.FieldConfig fieldConfig);

}