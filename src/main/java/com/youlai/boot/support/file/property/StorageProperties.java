package com.youlai.boot.support.file.property;

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
 * @author Ray.Hao
 * @since 4.8.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    // 存储类型，决定激活哪个 FileService 实现类
    private String type;

    // 上传限制
    private Upload upload = new Upload();

    /**
     * 上传限制配置嵌套类
     */
    @Data
    public static class Upload {

        // 单文件大小上限，如 50MB
        private String maxFileSize;

        // 允许上传的文件扩展名白名单
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