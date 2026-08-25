package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储配置。
 * <p>
 * 绑定前缀 {@code file-storage}，承载存储类型与上传限制；
 * rustfs / aliyun / local 子配置由各自的 {@code FileService} 实现类绑定。
 *
 * @author youlai.tech
 */
@Data
@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    /**
     * 存储类型，决定激活哪个 FileService 实现。
     * <p>
     * 可选值：rustfs、aliyun、local。
     */
    private String type;

    /** 上传限制 */
    private Upload upload = new Upload();

    /**
     * 上传限制，绑定前缀 {@code file-storage.upload}。
     */
    @Data
    public static class Upload {

        /**
         * 单文件大小上限，如 50MB。
         * <p>
         * 同时作为 spring.servlet.multipart 的上限，单一来源。
         */
        private String maxFileSize;

        /**
         * 允许的文件扩展名白名单，置空表示不限制。
         */
        private List<String> allowedExtensions;
    }

    /**
     * 允许扩展名集合（小写、不含点）；空集合表示不限制。
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
