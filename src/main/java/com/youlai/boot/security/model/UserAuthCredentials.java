package com.youlai.boot.security.model;

import lombok.Data;
import java.util.Set;

/**
 * 用户认证凭证信息
 *
 * @author Ray.Hao
 * @since 2022/10/22
 */
@Data
public class UserAuthCredentials {

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
     * 用户密码
     */
    private String password;

    /**
     * 状态（1:启用；0:禁用）
     */
    private Integer status;

    /**
     * 用户所属的角色集合
     */
    private Set<String> roles;

    /**
     * 数据权限范围，用于控制用户可以访问的数据级别
     *
     * @see com.youlai.boot.common.enums.DataScopeEnum
     */
    private Integer dataScope;

}
