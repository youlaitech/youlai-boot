package com.youlai.boot.module.workflow.config;

import com.youlai.boot.module.workflow.listener.ProcessCompletedListener;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 加载真实公共与环境配置，验证业务和 Flowable 的启用条件一致，不启动引擎或连接数据库
 */
class WorkflowConfigurationTest {

    private ApplicationContextRunner contextRunner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile)
                .withUserConfiguration(WorkflowEngineConfig.class, ProcessEngineConditionProbe.class)
                .withBean(ProcessCompletedListener.class, () -> mock(ProcessCompletedListener.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void workflowAndEngineAreDisabledByDefault(String profile) {
        contextRunner(profile).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WorkflowEngineConfig.class);
            assertThat(context).doesNotHaveBean(ProcessEngineConditionProbe.class);
            assertThat(context.getEnvironment().getProperty("flowable.process.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("flowable.idm.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("flowable.eventregistry.enabled", Boolean.class)).isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void oneWorkflowSwitchEnablesBusinessAndEngineConditions(String profile) {
        contextRunner(profile).withPropertyValues("flowable.process.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WorkflowEngineConfig.class);
            assertThat(context).hasSingleBean(ProcessEngineConditionProbe.class);
            assertThat(context.getEnvironment().getProperty("flowable.process.enabled", Boolean.class)).isTrue();
            assertThat(context.getEnvironment().getProperty("flowable.idm.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("flowable.eventregistry.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("flowable.database-schema-update"))
                    .isEqualTo("dev".equals(profile) ? "true" : "false");
        });
    }

    // 使用 Flowable 自己的条件注解，避免测试复制第三方开关逻辑，也不实例化真实流程引擎
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProcessEngine
    static class ProcessEngineConditionProbe {
    }
}
