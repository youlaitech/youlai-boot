package com.youlai.boot.system.model.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户查询对象")
public class UserQuery extends BaseQuery {

    @Schema(description = "关键字(用户名/昵称/手机号)")
    private String keywords;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "角色ID")
    private List<Long> roleIds;

    @Schema(description = "创建时间范围")
    private List<String> createTime;

    @JsonIgnore
    @Schema(hidden = true)
    private Boolean isRoot;
}
