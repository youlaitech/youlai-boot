package com.youlai.boot.framework.security.model;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 安全模块用户数据 POJO。
 *
 * @author Ray.Hao
 * @since 2025/12/16
 */
@Data
public class SecurityUser {

    // 用户ID
    private Long userId;

    // 用户名
    private String username;

    // 昵称
    private String nickname;

    // 部门ID
    private Long deptId;

    // 密码（加密后）
    private String password;

    // 状态（1:启用 其它:禁用）
    private Integer status;

    // 头像 URL
    private String avatar;

    // 角色编码集合
    private Set<String> roles;

    // 数据权限列表
    private List<RoleDataScope> dataScopes;
}
