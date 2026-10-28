package com.youlai.boot.system.model.query;

import com.youlai.boot.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户列表和导出共用的筛选及排序
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户分页查询对象")
public class UserQuery extends BaseQuery {

    @Schema(description = "排序规则，重复参数按优先级排列；方向为 asc 或 desc", example = "createTime,desc")
    private String[] sort;

    @Schema(description = "关键字(用户名/昵称/手机号)")
    private String keywords;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "角色ID")
    private List<Long> roleIds;

    // 下标 0、1 分别为起止时间，日期值由查询补齐当天边界
    @Schema(description = "创建时间范围，格式为 [开始时间, 结束时间]")
    private List<String> createTime;
}
