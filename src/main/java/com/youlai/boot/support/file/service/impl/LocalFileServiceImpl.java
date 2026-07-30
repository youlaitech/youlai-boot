package com.youlai.boot.support.file.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 本地文件存储服务。
 * <p>
 * 当 {@code file-storage.type=local} 时生效。
 * 文件写入本地磁盘 {@code file-storage.local.path} 下按日期分目录存储。
 * URL 为相对于存储根目录的路径，如 {@code /yyyyMMdd/uuid.suffix}。
 */
@Data
@Slf4j
@Component
@ConditionalOnProperty(value = "file-storage.type", havingValue = "local")
@ConfigurationProperties(prefix = "file-storage.local")
@RequiredArgsConstructor
public class LocalFileServiceImpl implements FileService {

    /** 本地存储根目录，来自 {@code file-storage.local.path} */
    @Value("${file-storage.local.path}")
    private String path;

    /**
     * 上传文件到本地磁盘，按日期分目录，返回相对于存储根目录的路径。
     */
    @Override
    public FileInfo uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        String folder = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String filePrefix = path.endsWith(File.separator) ? path : path + File.separator;
        try (InputStream inputStream = file.getInputStream()) {
            FileUtil.writeFromStream(inputStream, filePrefix + folder + File.separator + fileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败");
        }
        String fileUrl = File.separator + folder + File.separator + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    /**
     * 删除本地文件。路径是目录或不存在时返回 false。
     *
     * @param filePath 相对于存储根目录的文件路径
     * @return 删除成功返回 true
     */
    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        if (FileUtil.isDirectory(path + filePath)) {
            return false;
        }
        return FileUtil.del(path + filePath);
    }
}
