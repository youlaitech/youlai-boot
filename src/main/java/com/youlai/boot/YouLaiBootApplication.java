package com.youlai.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * <p>等价于 @SpringBootApplication，但显式声明 @ComponentScan 以排除工作流包，
 * 改由 {@link com.youlai.boot.module.config.WorkflowModuleConfig} 按 flowable.enabled 决定是否扫描</p>
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@ConfigurationPropertiesScan
@EnableScheduling
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.youlai.boot", excludeFilters = {
        // 保留 @SpringBootApplication 默认的排除器
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.youlai\\.boot\\.module\\.workflow\\..*")
})
public class YouLaiBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(YouLaiBootApplication.class, args);
    }

}
