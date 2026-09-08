package com.youlai.boot.module.form.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 表单数据VO（分页行与详情回显共用：数据只读，无独立表单对象）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单数据对象")
@Getter
@Setter
public class FormDataVO {

    @Schema(description = "数据ID")
    private Long id;

    @Schema(description = "表单版本（提交时快照）")
    private Integer formVersion;

    @Schema(description = "表单数据（field -> value 映射的 JSON 字符串）")
    private String dataJson;

    @Schema(description = "回显规则（提交时版本的快照，严格回显用；仅详情接口填充）")
    private String formJson;

    @Schema(description = "回显全局配置（提交时版本的快照；仅详情接口填充）")
    private String optionsJson;

    @Schema(description = "提交人ID（匿名提交为空）")
    private Long createBy;

    @Schema(description = "提交人昵称（匿名提交为空）")
    private String createByName;

    @Schema(description = "提交时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
