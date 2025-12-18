package com.youlai.boot.system.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serial;
import java.io.Serializable;

/**
 * 系统配置视图对象
 *
 * @author Theo
 * @since 2024-07-30 14:49
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Schema(description = "系统配置Vo")
public class ConfigVo {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "描述、备注")
    private String remark;
}
