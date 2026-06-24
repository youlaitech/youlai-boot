package com.youlai.boot.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.youlai.boot.file.service.FileService;
import com.youlai.boot.file.model.FileInfo;
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

@Component
@ConditionalOnProperty(value = "oss.type", havingValue = "aliyun")
@ConfigurationProperties(prefix = "oss.aliyun")
@RequiredArgsConstructor
@Data
public class AliyunFileServiceImpl implements FileService {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    private OSS aliyunOssClient;

    @PostConstruct
    public void init() {
        aliyunOssClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

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
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
            aliyunOssClient.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败");
        }
        String fileUrl = "https://" + bucketName + "." + endpoint + "/" + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    @Override
    public boolean deleteFile(String filePath) {
        Assert.notBlank(filePath, "删除文件路径不能为空");
        String fileHost = "https://" + bucketName + "." + endpoint;
        String fileName = filePath.substring(fileHost.length() + 1);
        aliyunOssClient.deleteObject(bucketName, fileName);
        return true;
    }
}
