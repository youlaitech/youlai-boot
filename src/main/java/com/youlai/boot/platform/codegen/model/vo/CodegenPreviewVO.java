package com.youlai.boot.platform.codegen.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "代码生成代码预览Vo")
@Data
public class CodegenPreviewVo {

    @Schema(description = "生成文件路径")
    private String path;

    @Schema(description = "生成文件名称",example = "SysUser.java" )
    private String fileName;

    @Schema(description = "生成文件内容")
    private String content;

}
