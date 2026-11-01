package com.youlai.boot.support.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 阿里云 OSS 文件存储服务
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "storage.type", havingValue = "aliyun")
@ConfigurationProperties(prefix = "storage.aliyun")
@RequiredArgsConstructor
@Data
public class AliyunFileServiceImpl implements FileService {

    /** OSS 地域节点，如 oss-cn-hangzhou.aliyuncs.com，兼容带 http(s):// 前缀写法 */
    private String endpoint;
    /** 访问密钥 ID */
    private String accessKeyId;
    /** 访问密钥 Secret */
    private String accessKeySecret;
    /** 存储空间名称 */
    private String bucket;

    @Setter(AccessLevel.NONE)
    private OSS aliyunOssClient;

    @PostConstruct
    public void init() {
        aliyunOssClient = new OSSClientBuilder().build(endpointWithProtocol(), accessKeyId, accessKeySecret);
    }

    /**
     * 上传文件到阿里云 OSS，按日期分目录，返回 {@code 协议://bucket.endpoint/path} 格式的 URL
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
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败", e);
        }
        String fileUrl = getFileHost() + "/" + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    /**
     * 从完整 URL 中提取文件名后调用 OSS 删除
     *
     * @param filePath 文件完整 URL
     * @return 删除成功返回 true
     */
    @Override
    public boolean deleteFile(String filePath) {
        Assert.notBlank(filePath, "删除文件路径不能为空");
        String fileName = filePath.substring(getFileHost().length() + 1);
        aliyunOssClient.deleteObject(bucket, fileName);
        return true;
    }

    /**
     * 补全 endpoint 协议前缀：已带 {@code http://} 或 {@code https://} 的仅去除尾部 {@code /}，
     * 纯域名写法默认补 {@code https://}，避免 OSS 客户端对无协议 endpoint 默认走 http 明文传输
     */
    private String endpointWithProtocol() {
        String trimmed = StrUtil.trim(endpoint);
        if (!StrUtil.startWithIgnoreCase(trimmed, "http://") && !StrUtil.startWithIgnoreCase(trimmed, "https://")) {
            trimmed = "https://" + trimmed;
        }
        return StrUtil.removeSuffix(trimmed, "/");
    }

    /**
     * 获取文件访问主机地址（含协议），如 {@code https://bucket.oss-cn-hangzhou.aliyuncs.com}
     */
    private String getFileHost() {
        String endpointWithProtocol = endpointWithProtocol();
        int schemeEnd = endpointWithProtocol.indexOf("://");
        String protocol = endpointWithProtocol.substring(0, schemeEnd);
        String host = endpointWithProtocol.substring(schemeEnd + "://".length());
        return protocol + "://" + bucket + "." + host;
    }
}
