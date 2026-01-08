package com.youlai.boot.common.base;

import com.youlai.boot.common.annotation.ValidField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema
public class BaseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页记录数", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @ValidField(allowedValues = {"create_time", "update_time"})
    private String sortBy;

    @Schema(description = "排序方式（正序:ASC；反序:DESC）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String order;

    public boolean isPaged() {
        return pageNum != null && pageSize != null && pageSize > 0;
    }
}
