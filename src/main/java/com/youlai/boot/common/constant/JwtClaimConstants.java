package com.youlai.boot.common.constant;

/**
 * JWT Claims声明常量
 *
 * @author haoxr
 * @since 2023/11/24
 */
public interface JwtClaimConstants {

    // 令牌类型
    String TOKEN_TYPE = "tokenType";

    // 用户ID
    String USER_ID = "userId";

    // 部门ID
    String DEPT_ID = "deptId";

    // 数据权限列表
    String DATA_SCOPES = "dataScopes";

    // 角色编码集合（不带 ROLE_ 前缀）
    String ROLES = "roles";

    // Token 版本号
    String TOKEN_VERSION = "tokenVersion";

}
