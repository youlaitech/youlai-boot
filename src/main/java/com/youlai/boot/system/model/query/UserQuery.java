package com.youlai.boot.system.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户分页查询对象")
public class UserQuery extends BaseQuery {

    @Schema(description = "关键字(用户名/昵称/手机号)")
    private String keywords;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "角色ID")
    private List<Long> roleIds;

    /**
     * 创建时间范围，下标 0 为开始时间、下标 1 为结束时间，只传日期时自动补全为当天 00:00:00 / 23:59:59
     */
    @Schema(description = "创建时间范围，格式为 [开始时间, 结束时间]")
    private List<String> createTime;
}
