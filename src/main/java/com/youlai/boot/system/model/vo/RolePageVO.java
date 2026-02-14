package com.youlai.boot.system.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description ="角色分页对象")
@Data
public class RolePageVO {

    @Schema(description="角色ID")
    private Long id;

    @Schema(description="角色名称")
    private String name;

    @Schema(description="角色编码")
    private String code;

    @Schema(description="角色状态")
    private Integer status;

    @Schema(description="排序")
    private Integer sort;

    @Schema(description="数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)")
    private Integer dataScope;

    @Schema(description="数据权限名称")
    private String dataScopeLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}