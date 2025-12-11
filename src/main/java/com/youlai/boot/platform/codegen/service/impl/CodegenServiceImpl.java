package com.youlai.boot.platform.codegen.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.platform.codegen.enums.JavaTypeEnum;
import com.youlai.boot.config.property.CodegenProperties;
import com.youlai.boot.platform.codegen.service.GenTableService;
import com.youlai.boot.platform.codegen.service.GenTableColumnService;
import com.youlai.boot.platform.codegen.service.CodegenService;
import com.youlai.boot.core.exception.BusinessException;
import com.youlai.boot.platform.codegen.mapper.DatabaseMapper;
import com.youlai.boot.platform.codegen.model.entity.GenTable;
import com.youlai.boot.platform.codegen.model.entity.GenTableColumn;
import com.youlai.boot.platform.codegen.model.query.TablePageQuery;
import com.youlai.boot.platform.codegen.model.vo.CodegenPreviewVO;
import com.youlai.boot.platform.codegen.model.vo.TablePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据库服务实现类
 *
 * @author Ray
 * @since 2.10.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodegenServiceImpl implements CodegenService {

    private final DatabaseMapper databaseMapper;
    private final CodegenProperties codegenProperties;
    private final GenTableService genTableService;
    private final GenTableColumnService genTableColumnService;

    /**
     * 数据表分页列表
     *
     * @param queryParams 查询参数
     * @return 分页结果
     */
    public Page<TablePageVO> getTablePage(TablePageQuery queryParams) {
        Page<TablePageVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        // 设置排除的表
        List<String> excludeTables = codegenProperties.getExcludeTables();
        queryParams.setExcludeTables(excludeTables);

        return databaseMapper.getTablePage(page, queryParams);
    }

    /**
     * 获取预览生成代码
     *
     * @param tableName 表名
     * @return 预览数据
     */
    @Override
    public List<CodegenPreviewVO> getCodegenPreviewData(String tableName, String pageType) {

        List<CodegenPreviewVO> list = new ArrayList<>();

        GenTable genTable = genTableService.getOne(new LambdaQueryWrapper<GenTable>()
                .eq(GenTable::getTableName, tableName)
        );
        if (genTable == null) {
            throw new BusinessException("未找到表生成配置");
        }

        List<GenTableColumn> fieldConfigs = genTableColumnService.list(new LambdaQueryWrapper<GenTableColumn>()
                .eq(GenTableColumn::getTableId, genTable.getId())
                .orderByAsc(GenTableColumn::getFieldSort)

        );
        if (CollectionUtil.isEmpty(fieldConfigs)) {
            throw new BusinessException("未找到字段生成配置");
        }

        // 遍历模板配置
        Map<String, CodegenProperties.TemplateConfig> templateConfigs = codegenProperties.getTemplateConfigs();
        for (Map.Entry<String, CodegenProperties.TemplateConfig> templateConfigEntry : templateConfigs.entrySet()) {
            CodegenPreviewVO previewVO = new CodegenPreviewVO();

            CodegenProperties.TemplateConfig templateConfig = templateConfigEntry.getValue();

            /* 1. 生成文件名 UserController */
            // User Role Menu Dept
            String entityName = genTable.getEntityName();
            // Controller Service Mapper Entity
            String templateName = templateConfigEntry.getKey();
            // .java .ts .vue
            String extension = templateConfig.getExtension();

            // 文件名 UserController.java
            String fileName = getFileName(entityName, templateName, extension);
            previewVO.setFileName(fileName);

            /* 2. 生成文件路径 */
            // 包名：com.youlai.boot
            String packageName = genTable.getPackageName();
            // 模块名：system
            String moduleName = genTable.getModuleName();
            // 子包名：controller
            String subpackageName = templateConfig.getSubpackageName();
            // 组合成文件路径：src/main/java/com/youlai/boot/system/controller
            String filePath = getFilePath(templateName, moduleName, packageName, subpackageName, entityName);
            previewVO.setPath(filePath);

            /* 3. 生成文件内容 */
            // 将模板文件中的变量替换为具体的值 生成代码内容
            // 优先使用保存的 ui，没有则使用请求参数
            String finalType = StrUtil.blankToDefault(genTable.getPageType(), pageType);
            String content = getCodeContent(templateConfig, genTable, fieldConfigs, finalType);
            previewVO.setContent(content);

            list.add(previewVO);
        }
        return list;
    }

    /**
     * 生成文件名
     *
     * @param entityName   实体类名 UserController
     * @param templateName 模板名 Entity
     * @param extension    文件后缀 .java
     * @return 文件名
     */
    private String getFileName(String entityName, String templateName, String extension) {
        if ("Entity".equals(templateName)) {
            return entityName + extension;
        } else if ("MapperXml".equals(templateName)) {
            return entityName + "Mapper" + extension;
        } else if ("API".equals(templateName)) {
            // 生成 user-api.ts 命名
            return StrUtil.toSymbolCase(entityName, '-') + "-api" + extension;
        } else if ("VIEW".equals(templateName)) {
            return "index.vue";
        }
        return entityName + templateName + extension;
    }

    /**
     * 生成文件路径
     *
     * @param templateName   模板名 Entity
     * @param moduleName     模块名 system
     * @param packageName    包名 com.youlai
     * @param subPackageName 子包名 controller
     * @param entityName     实体类名 UserController
     * @return 文件路径 src/main/java/com/youlai/system/controller
     */
    private String getFilePath(String templateName, String moduleName, String packageName, String subPackageName, String entityName) {
        String path;
        if ("MapperXml".equals(templateName)) {
            path = (codegenProperties.getBackendAppName()
                    + File.separator
                    + "src" + File.separator + "main" + File.separator + "resources"
                    + File.separator + subPackageName
                    + File.separator + moduleName
            );
        } else if ("API".equals(templateName)) {
            // path = "src/api/system";
            path = (codegenProperties.getFrontendAppName()
                    + File.separator + "src"
                    + File.separator + subPackageName
                    + File.separator + moduleName
            );
        } else if ("VIEW".equals(templateName)) {
            // path = "src/views/system/user";
            path = (codegenProperties.getFrontendAppName()
                    + File.separator + "src"
                    + File.separator + subPackageName
                    + File.separator + moduleName
                    + File.separator + StrUtil.toSymbolCase(entityName, '-')
            );
        } else {
            path = (codegenProperties.getBackendAppName()
                    + File.separator
                    + "src" + File.separator + "main" + File.separator + "java"
                    + File.separator + packageName
                    + File.separator + moduleName
                    + File.separator + subPackageName
            );
        }

        // subPackageName = model.entity => model/entity
        path = path.replace(".", File.separator);

        return path;
    }

    /**
     * 生成代码内容
     *
     * @param templateConfig 模板配置
     * @param genConfig      生成配置
     * @param fieldConfigs   字段配置
     * @return 代码内容
     */
    private String getCodeContent(CodegenProperties.TemplateConfig templateConfig, GenTable genTable, List<GenTableColumn> fieldConfigs, String pageType) {

        Map<String, Object> bindMap = new HashMap<>();

        String entityName = genTable.getEntityName();

        bindMap.put("packageName", genTable.getPackageName());
        bindMap.put("moduleName", genTable.getModuleName());
        bindMap.put("subpackageName", templateConfig.getSubpackageName());
        bindMap.put("date", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm"));
        bindMap.put("entityName", entityName);
        bindMap.put("tableName", genTable.getTableName());
        bindMap.put("author", genTable.getAuthor());
        bindMap.put("lowerFirstEntityName", StrUtil.lowerFirst(entityName)); // UserTest → userTest
        bindMap.put("kebabCaseEntityName", StrUtil.toSymbolCase(entityName, '-')); // UserTest → user-test
        bindMap.put("businessName", genTable.getBusinessName());
        bindMap.put("fieldConfigs", fieldConfigs);

        boolean hasLocalDateTime = false;
        boolean hasBigDecimal = false;
        boolean hasRequiredField = false;

        for (GenTableColumn fieldConfig : fieldConfigs) {

            if ("LocalDateTime".equals(fieldConfig.getFieldType())) {
                hasLocalDateTime = true;
            }
            if ("BigDecimal".equals(fieldConfig.getFieldType())) {
                hasBigDecimal = true;
            }
            if (ObjectUtil.equals(fieldConfig.getIsRequired(), 1)) {
                hasRequiredField = true;
            }
            fieldConfig.setTsType(JavaTypeEnum.getTsTypeByJavaType(fieldConfig.getFieldType()));
        }

        bindMap.put("hasLocalDateTime", hasLocalDateTime);
        bindMap.put("hasBigDecimal", hasBigDecimal);
        bindMap.put("hasRequiredField", hasRequiredField);

        TemplateEngine templateEngine = TemplateUtil.createEngine(new TemplateConfig("templates", TemplateConfig.ResourceMode.CLASSPATH));
        // 根据 ui 选择不同的前端页面模板：默认 index.vue.vm；封装版使用 index.curd.vue.vm
        String path = templateConfig.getTemplatePath();
        if ("curd".equalsIgnoreCase(pageType) && path.endsWith("index.vue.vm")) {
            path = path.replace("index.vue.vm", "index.curd.vue.vm");
        }
        Template template = templateEngine.getTemplate(path);

        return template.render(bindMap);
    }

    /**
     * 下载代码
     *
     * @param tableNames 表名数组，支持多张表。
     * @return 压缩文件字节数组
     */
    @Override
    public byte[] downloadCode(String[] tableNames, String ui) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {

            // 遍历每个表名，生成对应的代码并压缩到 zip 文件中
            for (String tableName : tableNames) {
                generateAndZipCode(tableName, zip, ui);
            }
            // 确保所有压缩数据写入输出流，避免数据残留在内存缓冲区引发的数据不完整
            zip.finish();
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error while generating zip for code download", e);
            throw new RuntimeException("Failed to generate code zip file", e);
        }
    }

    /**
     * 根据表名生成代码并压缩到zip文件中
     *
     * @param tableName 表名
     * @param zip       压缩文件输出流
     */
    private void generateAndZipCode(String tableName, ZipOutputStream zip, String ui) {
        List<CodegenPreviewVO> codePreviewList = getCodegenPreviewData(tableName, ui);

        for (CodegenPreviewVO codePreview : codePreviewList) {
            String fileName = codePreview.getFileName();
            String content = codePreview.getContent();
            String path = codePreview.getPath();

            try {
                // 创建压缩条目
                ZipEntry zipEntry = new ZipEntry(path + File.separator + fileName);
                zip.putNextEntry(zipEntry);

                // 写入文件内容
                zip.write(content.getBytes(StandardCharsets.UTF_8));

                // 关闭当前压缩条目
                zip.closeEntry();

            } catch (IOException e) {
                log.error("Error while adding file {} to zip", fileName, e);
            }
        }
    }

}
