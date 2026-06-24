package com.youlai.boot.security.filter;

import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.security.filter.TokenAuthenticationFilter;
import com.youlai.boot.framework.security.token.TokenManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private TokenManager tokenManager;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_clear_context_and_return_unauthorized_when_token_validation_fails() throws Exception {
        String token = "invalid-token";
        when(tokenManager.validateToken(token)).thenThrow(new IllegalStateException("token store unavailable"));
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken("old", null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader(HttpHeaders.AUTHORIZATION, SecurityConstants.BEARER_TOKEN_PREFIX + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TokenAuthenticationFilter(tokenManager).doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains(ResultCode.ACCESS_TOKEN_INVALID.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
