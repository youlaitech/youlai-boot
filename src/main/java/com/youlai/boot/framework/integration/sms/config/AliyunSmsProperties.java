package com.youlai.boot.framework.integration.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 阿里云短信配置
 *
 * @author Ray
 * @since 2024/8/17
 */
@Configuration
@ConfigurationProperties(prefix = "sms.aliyun")
@Data
public class AliyunSmsProperties {

    /** Access Key ID */
    private String accessKeyId;

    /** Access Key Secret */
    private String accessKeySecret;

    /** API 域名，如 dysmsapi.aliyuncs.com */
    private String domain;

    /** 区域，如 cn-shanghai */
    private String regionId;

    /** 短信签名 */
    private String signName;

    /** 短信模板集合，key 为模板用途(login/register)，value 为模板CODE */
    private Map<String, String> templates;

}
