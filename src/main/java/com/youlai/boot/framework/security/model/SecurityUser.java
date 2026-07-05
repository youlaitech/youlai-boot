package com.youlai.boot.framework.security.model;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 安全模块用户数据 POJO。
 * <p>
 * 作为端口接口的返回类型，承载用户认证所需的全部数据。
 * 纯 JDK 类型，无 system 模块依赖，可直接序列化。
 *
 * @author Ray.Hao
 * @since 2025/12/16
 */
@Data
public class SecurityUser {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 状态（1:启用 其它:禁用）
     */
    private Integer status;

    /**
     * 角色编码集合
     */
    private Set<String> roles;

    /**
     * 数据权限列表
     * <p>
     * 存储用户所有角色的数据权限范围，用于实现多角色权限合并（并集策略）
     */
    private List<RoleDataScope> dataScopes;
}
