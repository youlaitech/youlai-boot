package com.youlai.boot.framework.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogExecutorConfigTest {

    @Test
    void should_create_operation_log_executor() {
        Executor executor = new OperationLogExecutorConfig().operationLogExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("operation-log-");
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(1);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(2);
        assertThat(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(1000);
        assertThat(taskExecutor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        taskExecutor.shutdown();
    }
}
