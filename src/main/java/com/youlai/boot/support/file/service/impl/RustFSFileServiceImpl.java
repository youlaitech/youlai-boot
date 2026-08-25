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
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * RustFS 文件存储服务（S3 协议）。
 * <p>
 * 当 {@code file-storage.type=rustfs} 时生效。通过 AWS SDK 的 S3Client
 * 连接 RustFS（S3 兼容对象存储），IP 端点必须走 path-style 寻址。
 * 上传文件按日期分目录存储，路径格式：{@code yyyyMMdd/uuid.suffix}。
 * <ul>
 *   <li>配置了 {@code file-storage.rustfs.domain} 时直接用域名拼接 URL</li>
 *   <li>未配置则用预签名 GET URL（去掉 query 参数后的纯路径）</li>
 * </ul>
 *
 * @author youlai.tech
 */
@Component
@ConditionalOnProperty(value = "file-storage.type", havingValue = "rustfs")
@ConfigurationProperties(prefix = "file-storage.rustfs")
@RequiredArgsConstructor
@Data
@Slf4j
public class RustFSFileServiceImpl implements FileService {

    /** S3 对象存储服务端点，如 {@code http://localhost:9000} */
    private String endpoint;
    /** 访问密钥 */
    private String accessKey;
    /** 秘密密钥 */
    private String secretKey;
    /** 存储桶名称 */
    private String bucket;
    /** 自定义域名（可选），配置后不再使用预签名 URL */
    private String domain;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        // IP 端点必须走 path-style：桶名作为路径而不是子域名（public.IP 无法被 DNS 解析）
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                // 非 AWS 服务必填，值仅区分区域，任意即可
                .region(Region.US_EAST_1)
                .serviceConfiguration(s3Config)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .serviceConfiguration(s3Config)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /**
     * 上传文件到 RustFS，按日期分目录，自动创建存储桶，返回文件访问 URL。
     */
    @Override
    @SneakyThrows
    public FileInfo uploadFile(MultipartFile file) {
        createBucketIfAbsent(bucket);
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String dateFolder = DateUtil.format(LocalDateTime.now(), "yyyyMMdd");
        String fileName = IdUtil.simpleUUID() + "." + suffix;

        try {
            byte[] bytes = file.getBytes();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(dateFolder + "/" + fileName)
                    .contentType(file.getContentType())
                    .build();
            // 用固定长度的字节数组上传，避免流式分块签名在某些 S3 兼容服务上报校验和不匹配
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            String fileUrl;
            if (StrUtil.isBlank(domain)) {
                fileUrl = presignedUrl(dateFolder, fileName);
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
     * 生成预签名 GET URL 并去掉 query 参数，路径直达对象。
     * 依赖桶已开匿名只读（详见 createBucketIfAbsent 的策略），签名参数在 URL 中无实际作用。
     */
    private String presignedUrl(String dateFolder, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(dateFolder + "/" + fileName)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();
        String url = presigner.presignGetObject(presignRequest).url().toString();
        return url.substring(0, url.indexOf("?"));
    }

    /**
     * 删除文件。
     * <p>
     * 从完整 URL 中提取对象名后调用 S3 删除。
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
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(fileName).build());
            return true;
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new BusinessException(ResultCode.DELETE_FILE_EXCEPTION, e.getMessage());
        }
    }

    /** 公开只读策略：匿名可列桶与读对象，禁止任何匿名写入 */
    private static String publicBucketPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\","
                + "\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:GetObject\",\"s3:ListBucket\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "\",\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    /**
     * 存储桶不存在时自动创建，并设置为公开只读策略。
     */
    @SneakyThrows
    private void createBucketIfAbsent(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            // 桶不存在（404）时才创建；其他错误（如权限不足）放行由调用方抛出
            if (e.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucket).policy(publicBucketPolicy(bucket)).build());
            } else {
                throw e;
            }
        }
    }
}