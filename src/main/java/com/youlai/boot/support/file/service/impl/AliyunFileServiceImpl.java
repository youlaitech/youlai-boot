package com.youlai.boot.support.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 阿里云 OSS 文件存储服务。
 * <p>
 * 当 {@code file-storage.type=aliyun} 时生效。
 * 上传文件按日期分目录存储，URL 格式：{@code https://bucket.endpoint/yyyyMMdd/uuid.suffix}。
 */
@Component
@ConditionalOnProperty(value = "file-storage.type", havingValue = "aliyun")
@ConfigurationProperties(prefix = "file-storage.aliyun")
@RequiredArgsConstructor
@Data
public class AliyunFileServiceImpl implements FileService {

    /** OSS 地域节点，如 {@code oss-cn-hangzhou.aliyuncs.com} */
    private String endpoint;
    /** AccessKey ID */
    private String accessKeyId;
    /** AccessKey Secret */
    private String accessKeySecret;
    /** 存储空间名称 */
    private String bucket;

    private OSS aliyunOssClient;

    @PostConstruct
    public void init() {
        aliyunOssClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 上传文件到阿里云 OSS，按日期分目录，返回 {@code https://bucket.endpoint/path} 格式的 URL。
     */
    @Override
    @SneakyThrows
    public FileInfo uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String uuid = IdUtil.simpleUUID();
        String fileName = DateUtil.format(LocalDateTime.now(), "yyyyMMdd") + "/" + uuid + "." + suffix;
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, inputStream, metadata);
            aliyunOssClient.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败");
        }
        String fileUrl = "https://" + bucket + "." + endpoint + "/" + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    /**
     * 从完整 URL 中提取文件名后调用 OSS 删除。
     *
     * @param filePath 文件完整 URL
     * @return 删除成功返回 true
     */
    @Override
    public boolean deleteFile(String filePath) {
        Assert.notBlank(filePath, "删除文件路径不能为空");
        String fileHost = "https://" + bucket + "." + endpoint;
        String fileName = filePath.substring(fileHost.length() + 1);
        aliyunOssClient.deleteObject(bucket, fileName);
        return true;
    }
}
