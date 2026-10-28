package com.youlai.boot.auth.oidc.service;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.config.OidcProperties;
import com.youlai.boot.auth.oidc.model.OidcTicketContext;
import com.youlai.boot.common.enums.StatusEnum;
import com.youlai.boot.common.exception.TokenInvalidException;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.security.session.SessionService;
import com.youlai.boot.system.service.UserIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 将 OIDC 登录票据兑换为本地会话
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Service
@RequiredArgsConstructor
@ConditionalOnOidcEnabled
public class OidcLoginService {

    private final OidcTicketService oidcTicketService;
    private final OidcProperties oidcProperties;
    private final UserIdentityService userIdentityService;
    private final SessionService sessionService;

    /**
     * 消费票据，重新核对身份关联后创建本地会话
     */
    public TokenPair exchangeTicket(String ticket, HttpServletRequest request) {
        OidcTicketContext ticketContext = oidcTicketService.consume(ticket, request);

        var context = ticketContext.loginContext();
        if (!oidcProperties.getProvider().equals(context.provider())
                || !oidcProperties.getIssuer().equals(context.issuer())) {
            throw new TokenInvalidException(ResultCode.OIDC_LOGIN_FAILED);
        }

        // 领票后可能已解除或修改关联，不能直接沿用票据中的用户
        Long currentUserId = userIdentityService.findUserId(context.provider(), context.subject());
        if (!ticketContext.userId().equals(currentUserId)) {
            throw new TokenInvalidException(ResultCode.OIDC_ACCOUNT_NOT_BOUND);
        }

        SecurityUser securityUser = userIdentityService.getAuthInfoByUserId(currentUserId);
        if (securityUser == null) {
            throw new TokenInvalidException(ResultCode.OIDC_ACCOUNT_NOT_BOUND);
        }
        if (!StatusEnum.ENABLE.getValue().equals(securityUser.getStatus())) {
            throw new TokenInvalidException(ResultCode.ACCOUNT_FROZEN);
        }

        SecurityUserDetails userDetails = new SecurityUserDetails(securityUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        TokenPair tokenPair = sessionService.create(authentication, context);
        userIdentityService.markLogin(context.provider(), context.subject());
        return tokenPair;
    }
}
