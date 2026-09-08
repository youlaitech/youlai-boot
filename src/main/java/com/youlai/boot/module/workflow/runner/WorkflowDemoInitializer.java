package com.youlai.boot.module.workflow.runner;

import com.youlai.boot.module.workflow.service.WorkflowDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示流程启动初始化器
 *
 * <p>委托 {@link WorkflowDemoService} 幂等初始化，重置能力由管理端接口提供。
 * 由 flowable.demo.enabled 控制（默认开启），生产环境置 false 后本组件不注册，启动零开销</p>
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Component
@ConditionalOnProperty(name = "flowable.demo.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WorkflowDemoInitializer implements ApplicationRunner {

    private final WorkflowDemoService workflowDemoService;

    @Override
    public void run(ApplicationArguments args) {
        workflowDemoService.initIfAbsent();
    }

}
