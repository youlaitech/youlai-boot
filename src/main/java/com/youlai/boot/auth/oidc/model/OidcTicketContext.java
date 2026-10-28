package com.youlai.boot.auth.oidc.model;

import com.youlai.boot.framework.security.session.LoginContext;

/**
 * 一次性票据中的用户、外部身份与浏览器绑定摘要
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
public record OidcTicketContext(Long userId, LoginContext loginContext, String browserBinding) {
}
