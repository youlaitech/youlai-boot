package com.youlai.boot.auth.oidc.service;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.framework.security.session.LoginContext;
import com.youlai.boot.framework.security.session.LoginSession;
import com.youlai.boot.framework.security.session.SessionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * 检查会话所属的认证源是否仍然启用
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcSessionValidator implements SessionValidator {

    private final OidcProperties properties;

    @Override
    public LoginContext.Source source() {
        return LoginContext.Source.OIDC;
    }

    @Override
    public void validate(LoginSession session) {
        var context = session.context();
        if (!properties.isEnabled() || !properties.getProvider().equals(context.provider())
                || !properties.getIssuer().equals(context.issuer())) {
            throw new BadCredentialsException("OIDC provider is no longer available");
        }
    }
}
