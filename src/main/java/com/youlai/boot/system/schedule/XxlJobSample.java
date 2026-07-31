package com.youlai.boot.system.schedule;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 示例任务。
 * <p>
 * 需在调度中心配置同名任务后方可执行。
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Component
@Slf4j
public class XxlJobSample {

    @XxlJob("sampleJob")
    public void execute() {
        log.info("XXL-JOB 示例任务执行");
    }
}
