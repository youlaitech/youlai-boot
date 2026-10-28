package com.youlai.boot.support.realtime.security;

import com.youlai.boot.framework.security.port.PermissionPort;
import com.youlai.boot.framework.security.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import java.util.Set;

/**
 * 实时连接与 HTTP 请求共用会话校验，权限以服务端记录为准
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@RequiredArgsConstructor
public class RealtimeCredentialVerifier {
    private final SessionService sessionService;
    private final PermissionPort permissions;

    /**
     * 复用登录会话校验并提取实时连接所需权限
     */
    public VerifiedIdentity verify(String accessToken) {
        var session = sessionService.authenticate(accessToken);
        Set<String> roles = session.principal().roles();
        Set<String> perms = roles.isEmpty() ? Set.of() : permissions.getRolePerms(roles);
        boolean presenceAllowed = roles.contains("ROOT") || perms.stream()
                .anyMatch(perm -> PatternMatchUtils.simpleMatch(perm, "sys:online:view"));
        return new VerifiedIdentity(session.principal().userId(), presenceAllowed, session.accessExpiresAt());
    }

    /**
     * 已验证的用户身份、凭证期限及在线统计权限
     */
    public record VerifiedIdentity(Long userId, boolean presenceAllowed, java.time.Instant credentialExpiresAt) {
    }
}
