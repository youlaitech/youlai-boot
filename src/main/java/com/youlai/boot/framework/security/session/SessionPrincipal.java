package com.youlai.boot.framework.security.session;

import com.youlai.boot.framework.security.model.RoleDataScope;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;

import java.util.List;
import java.util.Set;

/**
 * 会话中的业务身份，不保存密码
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public record SessionPrincipal(Long userId, String username, Long deptId,
                               Set<String> roles, List<RoleDataScope> dataScopes) {

    public SessionPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        dataScopes = dataScopes == null ? List.of() : dataScopes.stream()
                .map(scope -> new RoleDataScope(scope.getRoleCode(), scope.getDataScope(),
                        scope.getCustomDeptIds() == null ? null : List.copyOf(scope.getCustomDeptIds())))
                .toList();
    }

    /**
     * 从业务账号提取会话身份与权限范围
     */
    public static SessionPrincipal from(SecurityUser user) {
        return new SessionPrincipal(user.getUserId(), user.getUsername(), user.getDeptId(),
                user.getRoles(), user.getDataScopes());
    }

    /**
     * 转换为不含密码的 Spring Security 用户详情
     */
    public SecurityUserDetails toUserDetails() {
        var details = new SecurityUserDetails();
        details.setUserId(userId);
        details.setUsername(username);
        details.setDeptId(deptId);
        details.setEnabled(true);
        details.setRoles(roles);
        details.setDataScopes(dataScopes);
        return details;
    }
}
