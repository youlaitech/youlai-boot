package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储顶层配置：存储类型与上传限制。
 * <p>
 * minio / aliyun / local 子树由各自 {@code FileService} 实现类绑定，
 * 此处仅承载 {@code type} 与 {@code upload}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    /** 存储类型：minio | aliyun | local */
    private String type;

    private Upload upload = new Upload();

    @Data
    public static class Upload {

        /** 单文件大小上限，如 50MB；同时作为 spring.servlet.multipart 的上限 */
        private String maxFileSize;

        /** 允许的文件扩展名白名单（置空表示不限制） */
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
