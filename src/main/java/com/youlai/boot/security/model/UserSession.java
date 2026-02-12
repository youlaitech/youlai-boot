package com.youlai.boot.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 用户会话信息
 * <p>
 * 存储在Token中的用户会话快照，包含用户身份、数据权限和角色权限信息。
 * 用于Redis-Token模式下的会话管理，支持在线用户查询和会话控制。
 *
 * @author wangtao
 * @since 2025/2/27 10:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 数据权限列表
     */
    private List<RoleDataScope> dataScopes;

    /**
     * 角色权限集合
     */
    private Set<String> roles;

}
