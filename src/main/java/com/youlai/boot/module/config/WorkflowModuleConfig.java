package com.youlai.boot.module.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流模块装配
 *
 * <p>仅当 flowable.enabled 为 true 时扫描工作流包，未配置视为关闭</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Configuration
@ConditionalOnProperty(name = "flowable.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.youlai.boot.module.workflow")
public class WorkflowModuleConfig {
}
