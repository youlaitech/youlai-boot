package com.youlai.boot.common.constant;

/**
 * JWT Claims声明常量
 * <p>
 * JWT Claims 属于 Payload 的一部分，包含了一些实体（通常指的用户）的状态和额外的元数据。
 *
 * @author haoxr
 * @since 2023/11/24
 */
public interface JwtClaimConstants {

    /**
     * 令牌类型
     */
    String TOKEN_TYPE = "tokenType";

    /**
     * 用户ID
     */
    String USER_ID = "userId";

    /**
     * 部门ID
     */
    String DEPT_ID = "deptId";

    /**
     * 数据权限列表
     * <p>
     * 存储用户所有角色的数据权限范围，用于实现多角色权限合并（并集策略）
     */
    String DATA_SCOPES = "dataScopes";

    /**
     * 权限(角色Code)集合
     */
    String AUTHORITIES = "authorities";

    /**
     * Token 版本号
     * <p>
     * 用于用户级会话失效，当用户修改密码、被禁用、强制下线时递增版本号，
     * 使该用户之前签发的所有 Token 失效。
     */
    String TOKEN_VERSION = "tokenVersion";

}
