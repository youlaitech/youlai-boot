package com.youlai.boot.module.workflow.runner;

import com.youlai.boot.module.workflow.service.WorkflowDemoService;
import com.youlai.boot.module.workflow.config.ConditionalOnWorkflowEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示流程启动初始化器
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@ConditionalOnWorkflowEnabled
@Component
@ConditionalOnProperty(name = "workflow.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WorkflowDemoInitializer implements ApplicationRunner {

    private final WorkflowDemoService workflowDemoService;

    @Override
    public void run(ApplicationArguments args) {
        workflowDemoService.initIfAbsent();
    }

}
