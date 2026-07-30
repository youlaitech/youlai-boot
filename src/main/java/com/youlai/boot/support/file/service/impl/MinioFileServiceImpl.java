package com.youlai.boot.support.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * MinIO 文件存储服务。
 * <p>
 * 当 {@code file-storage.type=minio} 时生效。
 * 上传文件按日期分目录存储，路径格式：{@code yyyyMMdd/uuid.suffix}。
 * <ul>
 *   <li>配置了 {@code file-storage.minio.domain} 时直接用域名拼接 URL</li>
 *   <li>未配置则用 MinIO 预签名 URL（去掉 query 参数后的纯路径）</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(value = "file-storage.type", havingValue = "minio")
@ConfigurationProperties(prefix = "file-storage.minio")
@RequiredArgsConstructor
@Data
@Slf4j
public class MinioFileServiceImpl implements FileService {

    /** MinIO 服务端点，如 {@code http://localhost:9000} */
    private String endpoint;
    /** 访问密钥 */
    private String accessKey;
    /** 秘密密钥 */
    private String secretKey;
    /** 存储桶名称 */
    private String bucket;
    /** 自定义域名（可选），配置后不再使用预签名 URL */
    private String domain;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传文件到 MinIO，按日期分目录，自动创建存储桶，返回文件访问 URL。
     */
    @Override
    public FileInfo uploadFile(MultipartFile file) {
        createBucketIfAbsent(bucket);
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String dateFolder = DateUtil.format(LocalDateTime.now(), "yyyyMMdd");
        String fileName = IdUtil.simpleUUID() + "." + suffix;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(dateFolder + "/" + fileName)
                    .contentType(file.getContentType())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);

            String fileUrl;
            if (StrUtil.isBlank(domain)) {
                GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(dateFolder + "/" + fileName)
                        .method(Method.GET)
                        .build();
                fileUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
                fileUrl = fileUrl.substring(0, fileUrl.indexOf("?"));
            } else {
                fileUrl = domain + "/" + bucket + "/" + dateFolder + "/" + fileName;
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(originalFilename);
            fileInfo.setUrl(fileUrl);
            return fileInfo;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BusinessException(ResultCode.UPLOAD_FILE_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 删除文件。
     * <p>
     * 从完整 URL 中提取文件名后调用 MinIO 删除。
     *
     * @param filePath 文件完整 URL
     * @return 删除成功返回 true
     */
    @Override
    public boolean deleteFile(String filePath) {
        Assert.notBlank(filePath, "删除文件路径不能为空");
        try {
            String fileName;
            if (StrUtil.isNotBlank(domain)) {
                fileName = filePath.substring(domain.length() + 1 + bucket.length() + 1);
            } else {
                fileName = filePath.substring(endpoint.length() + 1 + bucket.length() + 1);
            }
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(fileName).build());
            return true;
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new BusinessException(ResultCode.DELETE_FILE_EXCEPTION, e.getMessage());
        }
    }

    /** 生成公开读的存储桶策略 JSON */
    private static String publicBucketPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\","
                + "\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:ListBucketMultipartUploads\",\"s3:GetBucketLocation\",\"s3:ListBucket\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "\"]},"
                + "{\"Effect\":\"Allow\",\"" + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:ListMultipartUploadParts\",\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    /**
     * 存储桶不存在时自动创建，并设置为公开读策略。
     */
    @SneakyThrows
    private void createBucketIfAbsent(String bucket) {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket).config(publicBucketPolicy(bucket)).build());
        }
    }
}
