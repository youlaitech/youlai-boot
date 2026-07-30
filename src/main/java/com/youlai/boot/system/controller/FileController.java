package com.youlai.boot.system.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.config.property.FileStorageProperties;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 文件控制层
 *
 * @author Ray.Hao
 * @since 2022/10/16
 */
@Tag(name = "10.文件接口")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorageProperties fileStorageProperties;

    @PostMapping
    @Operation(summary = "文件上传")
    public Result<FileInfo> uploadFile(
            @Parameter(
                    name = "file",
                    description = "表单文件对象",
                    required = true,
                    in = ParameterIn.DEFAULT,
                    schema = @Schema(name = "file", format = "binary")
            )
            @RequestPart(value = "file") MultipartFile file
    ) {
        validateFileExtension(file);
        FileInfo fileInfo = fileService.uploadFile(file);
        return Result.success(fileInfo);
    }

    @DeleteMapping
    @Operation(summary = "文件删除")
    @SneakyThrows
    public Result<?> deleteFile(
            @Parameter(description = "文件路径") @RequestParam String filePath
    ) {
        boolean result = fileService.deleteFile(filePath);
        return Result.judge(result);
    }

    /**
     * 文件扩展名白名单校验：{@code allowed-extensions} 为空时不限制。
     */
    private void validateFileExtension(MultipartFile file) {
        Set<String> allowedExtensions = fileStorageProperties.getAllowedExtensions();
        if (allowedExtensions.isEmpty()) {
            return;
        }
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        if (StrUtil.isBlank(suffix) || !allowedExtensions.contains(suffix.toLowerCase())) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_EXCEPTION,
                    "不支持的文件类型: " + (StrUtil.isBlank(suffix) ? "" : "." + suffix));
        }
    }
}
