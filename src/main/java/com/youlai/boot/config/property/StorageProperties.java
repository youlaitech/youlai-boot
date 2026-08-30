package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储配置属性类
 *
 * <p>映射 application.yml 中 storage 前缀的配置；s3 / aliyun / local
 * 子配置由各自的 {@code FileService} 实现类绑定</p>
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 存储类型，决定激活哪个 FileService 实现类
     * <ul>
     *   <li>s3 - S3 协议兼容对象存储（当前部署 RustFS）</li>
     *   <li>aliyun - 阿里云 OSS</li>
     *   <li>local - 本地磁盘存储</li>
     * </ul>
     */
    private String type;

    /**
     * 上传限制
     */
    private Upload upload = new Upload();

    /**
     * 上传限制配置嵌套类
     */
    @Data
    public static class Upload {

        /**
         * 单文件大小上限，如 50MB
         * <p>同时作为 spring.servlet.multipart 的上限，保持单一来源</p>
         */
        private String maxFileSize;

        /**
         * 允许上传的文件扩展名白名单
         * <p>置空表示不限制</p>
         */
        private List<String> allowedExtensions;
    }

    /**
     * 获取规范化后的扩展名白名单
     *
     * @return 小写、不含点的扩展名集合；空集合表示不限制
     */
    public Set<String> getAllowedExtensions() {
        if (upload == null || upload.allowedExtensions == null) {
            return Collections.emptySet();
        }
        return upload.allowedExtensions.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith(".") ? s.substring(1) : s)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }
}
