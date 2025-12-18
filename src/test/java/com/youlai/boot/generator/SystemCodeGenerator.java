package com.youlai.boot.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;

import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;

import java.util.*;

/**
 * 系统模块代码生成
 * <p>
 * 代码生成、MySQL表生成代码、自动代码生成
 *
 * @author Ray Hao
 * @see <a href="https://baomidou.com/pages/981406">代码生成器配置新</a>
 * @since 2024/4/9
 */
public class SystemCodeGenerator {

    private static final DataSourceConfig.Builder DATA_SOURCE_CONFIG = new DataSourceConfig
            .Builder("jdbc:mysql://localhost:3306/youlai_boot?serverTimezone=Asia/Shanghai", "root", "123456");

    /**
     * 执行 run
     */
    public static void main(String[] args) {
        FastAutoGenerator.create(DATA_SOURCE_CONFIG)
                // 全局配置
                .globalConfig((scanner, builder) -> {
                    builder.outputDir(System.getProperty("user.dir") + "/src/main/java")
                            .author("Ray Hao") // 设置作者
                    ;
                })
                // 包配置
                .packageConfig(builder -> {
                            builder
                                    .parent("com.youlai.boot.system")
                                    .entity("model.entity")
                                    .mapper("mapper")
                                    .service("platform")
                                    .serviceImpl("platform.impl")
                                    .controller("controller")
                                    .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper"));
                        }
                )
                // 注入配置(设置扩展类的模板路径和包路径)
                .injectionConfig(consumer -> {
                    List<CustomFile> customFiles = new ArrayList<>();
                    customFiles.add(new CustomFile.Builder().fileName("Vo.java").templatePath("/templates/vo.java.vm").packageName("model.vo").build());
                    customFiles.add(new CustomFile.Builder().fileName("Dto.java").templatePath("/templates/dto.java.vm").packageName("model.dto").build());
                    customFiles.add(new CustomFile.Builder().fileName("Bo.java").templatePath("/templates/bo.java.vm").packageName("model.bo").build());
                    customFiles.add(new CustomFile.Builder().fileName("PageQuery.java").templatePath("/templates/query.java.vm").packageName("model.query").build());
                    customFiles.add(new CustomFile.Builder().fileName("PageVo.java").templatePath("/templates/pageVo.java.vm").packageName("model.vo").build());
                    customFiles.add(new CustomFile.Builder().fileName("Form.java").templatePath("/templates/form.java.vm").packageName("model.form").build());
                    customFiles.add(new CustomFile.Builder().fileName("Converter.java").templatePath("/templates/converter.java.vm").packageName("converter").build());
                    consumer.customFile(customFiles);
                    consumer.beforeOutputFile((tableInfo, objectMap) -> {
                        // 为每个表生成首字母小写的实体名
                        String entityName = tableInfo.getEntityName();
                        String lowerCaseEntity = entityName.substring(0, 1).toLowerCase() + entityName.substring(1);
                        // 注入自定义参数
                        objectMap.put("lowerFirstEntityName", lowerCaseEntity);

                    });

                })
                // 策略配置
                .strategyConfig((scanner, builder) -> {

                            builder.entityBuilder()
                                    .enableLombok() // 是否使用lombok
                                    //.enableFileOverride() // 开启覆盖已生成的文件
                                    .logicDeleteColumnName("deleted") // 逻辑删除字段名
                                    .enableRemoveIsPrefix() // 开启移除is前缀
                            ;

                            builder.mapperBuilder()
                                    .enableBaseColumnList()
                                    .enableBaseResultMap()
                            ;

                            builder.serviceBuilder()
                                    .formatServiceFileName("%sService"
                                    );


                            builder.addTablePrefix("sys_") // 过滤移除表前缀 sys_user 表生成的实体类 User.java
                                    .addInclude(scanner.apply("请输入表名，多个表名用,隔开"));
                        }
                )
                .execute()

        ;
    }
}
