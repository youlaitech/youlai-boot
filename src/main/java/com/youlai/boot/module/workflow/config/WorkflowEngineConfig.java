package com.youlai.boot.module.workflow.config;

import com.youlai.boot.module.workflow.listener.ProcessCompletedListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流引擎配置
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Configuration(proxyBeanMethods = false)
public class WorkflowEngineConfig {

    /**
     * 流程完成事件监听器登记到引擎全局（任意流程结束均派发，由监听器按定义 Key 路由业务后处理）
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processCompletedListenerRegistrar(
            ProcessCompletedListener listener) {
        return configuration -> {
            // 追加而非覆盖：保留引擎/starter 已登记的监听器
            List<FlowableEventListener> listeners = new ArrayList<>();
            if (configuration.getEventListeners() != null) {
                listeners.addAll(configuration.getEventListeners());
            }
            listeners.add(listener);
            configuration.setEventListeners(listeners);
        };
    }
}
