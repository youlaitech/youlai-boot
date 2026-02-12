package com.youlai.boot.plugin.mybatis;

import com.youlai.boot.common.annotation.DataPermission;

import java.util.List;

/**
 * 数据权限测试 Mapper
 * <p>
 * 用于测试数据权限拦截器的SQL注入功能
 */
public interface TestDataPermissionMapper {

    /**
     * 查询列表（带数据权限过滤）
     */
    @DataPermission
    List<Object> selectList();

    /**
     * 查询列表（不带数据权限过滤）
     */
    List<Object> selectListWithoutPermission();

    /**
     * 多表关联查询（指定别名）
     */
    @DataPermission(deptAlias = "u", userAlias = "u")
    List<Object> selectWithJoin();

    /**
     * 自定义列名查询（多表关联场景）
     */
    @DataPermission(
            deptAlias = "t",
            deptIdColumnName = "dept_id",
            userAlias = "t",
            userIdColumnName = "create_by"
    )
    List<Object> selectWithAlias();
}
