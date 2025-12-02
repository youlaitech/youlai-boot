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
     * 数据权限
     */
    String DATA_SCOPE = "dataScope";

    /**
     * 权限(角色Code)集合
     */
    String AUTHORITIES = "authorities";

    /**
     * 安全版本号，用于按用户失效历史令牌
     */
    String SECURITY_VERSION = "securityVersion";

}
