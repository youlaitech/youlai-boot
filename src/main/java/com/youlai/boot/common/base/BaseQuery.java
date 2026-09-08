package com.youlai.boot.common.base;

import com.youlai.boot.common.annotation.ValidField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询参数基类
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
@Data
@Schema
public class BaseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    @Schema(description = "每页记录数", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "10")
    @Min(value = 1, message = "每页记录数不能小于 1")
    @Max(value = 200, message = "每页记录数不能超过 200")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @ValidField(allowedValues = {"create_time", "update_time"})
    private String sortBy;

    @Schema(description = "排序方式（正序:asc；反序:desc，不区分大小写）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @ValidField(allowedValues = {"asc", "desc", "ASC", "DESC"})
    private String order;

    public boolean isPaged() {
        return pageNum != null && pageSize != null && pageSize > 0;
    }
}
