package com.youlai.boot.module.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 工作流关闭时排除 Flowable 引擎自动配置
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
public class WorkflowEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** 工作流开关属性 */
    private static final String WORKFLOW_ENABLED = "flowable.enabled";

    /** 自动配置排除列表属性 */
    private static final String AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

    /** Flowable 引擎相关的自动配置类 */
    private static final List<String> FLOWABLE_AUTOCONFIGURATIONS = List.of(
            "org.flowable.spring.boot.ProcessEngineAutoConfiguration",
            "org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 与 WorkflowModuleConfig 的条件一致：未配置视为关闭
        if ("true".equalsIgnoreCase(environment.getProperty(WORKFLOW_ENABLED))) {
            return;
        }
        // 合并已有的排除项，避免覆盖用户自己的 spring.autoconfigure.exclude
        List<String> excluded = new ArrayList<>(Binder.get(environment)
                .bind(AUTOCONFIGURE_EXCLUDE, Bindable.listOf(String.class))
                .orElseGet(List::of));
        for (String className : FLOWABLE_AUTOCONFIGURATIONS) {
            if (!excluded.contains(className)) {
                excluded.add(className);
            }
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(AUTOCONFIGURE_EXCLUDE, String.join(",", excluded));
        environment.getPropertySources().addFirst(new MapPropertySource("workflowDisabled", properties));
    }

    /**
     * 晚于 ConfigDataEnvironmentPostProcessor 执行，保证 profile 配置文件已加载
     */
    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
