package com.youlai.boot;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 分层依赖约束
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@AnalyzeClasses(packages = "com.youlai.boot")
class ArchitectureTest {

    @ArchTest
    static final ArchRule commonShouldNotDependOnUpperLayers =
            noClasses().that().resideInAPackage("com.youlai.boot.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.youlai.boot.config..",
                            "com.youlai.boot.framework..",
                            "com.youlai.boot.system..",
                            "com.youlai.boot.module..",
                            "com.youlai.boot.auth..",
                            "com.youlai.boot.codegen..",
                            "com.youlai.boot.support..")
                    .because("common 是纯共享层，只提供基础类型，不感知装配层、框架层和业务模块");

    @ArchTest
    static final ArchRule frameworkShouldNotDependOnBusinessModules =
            noClasses().that().resideInAPackage("com.youlai.boot.framework..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.youlai.boot.system..",
                            "com.youlai.boot.module..",
                            "com.youlai.boot.auth..",
                            "com.youlai.boot.codegen..")
                    .because("framework 提供通用技术能力，不依赖具体业务模块");

    @ArchTest
    static final ArchRule systemShouldNotDependOnCodegen =
            noClasses().that().resideInAPackage("com.youlai.boot.system..")
                    .should().dependOnClassesThat().resideInAPackage("com.youlai.boot.codegen..")
                    .because("依赖方向为 codegen -> system，system 不得反向依赖代码生成");

    @ArchTest
    static final ArchRule controllersShouldNotDependOnMappers =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
                    .because("Controller 只能通过 Service 访问数据层");

    @ArchTest
    static final ArchRule businessPackagesShouldBeFreeOfCycles =
            slices().matching("com.youlai.boot.(*)..")
                    .should().beFreeOfCycles()
                    .because("顶层包之间应单向依赖，出现环说明模块边界被绕过");
}
