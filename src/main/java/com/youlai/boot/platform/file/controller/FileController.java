package com.youlai.boot.platform.file.controller;

import com.youlai.boot.core.web.Result;
import com.youlai.boot.platform.file.service.FileService;
import com.youlai.boot.platform.file.model.FileInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
