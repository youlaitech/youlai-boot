package com.youlai.boot.security.token;

import com.youlai.boot.config.property.SecurityProperties;
import com.youlai.boot.security.model.AuthenticationToken;
import com.youlai.boot.security.model.RoleDataScope;
import com.youlai.boot.security.model.SysUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtTokenManager 单元测试
 *
 * @author Ray.Hao
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtTokenManager tokenManager;
    private SecurityProperties securityProperties;

    private static final String TEST_SECRET_KEY = "TestSecretKey01234567890123456789";
    private static final int ACCESS_TOKEN_TTL = 3600;
    private static final int REFRESH_TOKEN_TTL = 604800;

    @BeforeEach
    void setUp() {
        securityProperties = createSecurityProperties();
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);

        tokenManager = new JwtTokenManager(securityProperties, redisTemplate);
    }

    @Nested
    @DisplayName("Token 生成测试")
    class GenerateTokenTests {

        @Test
        @DisplayName("应成功生成有效的访问令牌和刷新令牌")
        void should_generate_valid_token() {
            Authentication authentication = createTestAuthentication();

            AuthenticationToken token = tokenManager.generateToken(authentication);

            assertThat(token).isNotNull();
            assertThat(token.getAccessToken()).isNotBlank();
            assertThat(token.getRefreshToken()).isNotBlank();
            assertThat(token.getTokenType()).isEqualTo("Bearer");
            assertThat(token.getExpiresIn()).isEqualTo(ACCESS_TOKEN_TTL);
            assertThat(token.getAccessToken()).isNotEqualTo(token.getRefreshToken());
        }

        @Test
        @DisplayName("生成的 Token 应包含用户信息")
        void should_contain_user_info() {
            Authentication authentication = createTestAuthentication();

            AuthenticationToken token = tokenManager.generateToken(authentication);
            Authentication parsed = tokenManager.parseToken(token.getAccessToken());

            assertThat(parsed).isNotNull();
            assertThat(parsed.getName()).isEqualTo("testuser");

            SysUserDetails userDetails = (SysUserDetails) parsed.getPrincipal();
            assertThat(userDetails.getUserId()).isEqualTo(1L);
            assertThat(userDetails.getDeptId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("生成的 Token 应包含角色权限")
        void should_contain_authorities() {
            Authentication authentication = createTestAuthentication();

            AuthenticationToken token = tokenManager.generateToken(authentication);
            Authentication parsed = tokenManager.parseToken(token.getAccessToken());

            Collection<?> authorities = parsed.getAuthorities();
            assertThat(authorities).hasSize(2);
            assertThat(authorities)
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("生成的 Token 应包含数据权限")
        void should_contain_data_scopes() {
            Authentication authentication = createTestAuthentication();

            AuthenticationToken token = tokenManager.generateToken(authentication);
            Authentication parsed = tokenManager.parseToken(token.getAccessToken());

            SysUserDetails userDetails = (SysUserDetails) parsed.getPrincipal();
            List<RoleDataScope> dataScopes = userDetails.getDataScopes();

            assertThat(dataScopes).hasSize(2);
            assertThat(dataScopes.get(0).getRoleCode()).isEqualTo("ADMIN");
            assertThat(dataScopes.get(0).getDataScope()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Token 校验测试")
    class ValidateTokenTests {

        @Test
        @DisplayName("有效 Token 校验应返回 true")
        void should_validate_valid_token() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            boolean isValid = tokenManager.validateToken(token.getAccessToken());

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("无效签名 Token 校验应返回 false")
        void should_reject_invalid_signature() {
            String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";

            boolean isValid = tokenManager.validateToken(invalidToken);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("刷新令牌校验应区分访问令牌")
        void should_distinguish_refresh_token() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            // 访问令牌不能作为刷新令牌使用
            boolean isValidRefreshToken = tokenManager.validateRefreshToken(token.getAccessToken());
            assertThat(isValidRefreshToken).isFalse();

            // 刷新令牌是有效的
            boolean isValidRefreshToken2 = tokenManager.validateRefreshToken(token.getRefreshToken());
            assertThat(isValidRefreshToken2).isTrue();
        }
    }

    @Nested
    @DisplayName("Token 撤销测试")
    class InvalidateTokenTests {

        @Test
        @DisplayName("撤销 Token 后校验应返回 false")
        void should_invalidate_token() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            // 先验证 Token 有效
            assertThat(tokenManager.validateToken(token.getAccessToken())).isTrue();

            // 撤销 Token
            tokenManager.invalidateToken(token.getAccessToken());

            // 验证 Redis 存储了撤销标记
            verify(valueOperations).set(anyString(), any(Boolean.class), anyLong(), any());
        }

        @Test
        @DisplayName("撤销空 Token 应安全处理")
        void should_handle_null_token() {
            tokenManager.invalidateToken(null);
            tokenManager.invalidateToken("");

            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("用户会话失效测试")
    class InvalidateUserSessionsTests {

        @Test
        @DisplayName("失效用户所有会话后，旧 Token 应无效")
        void should_invalidate_user_sessions() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            // 模拟用户会话失效后的 Redis 状态
            when(valueOperations.get(anyString())).thenReturn(System.currentTimeMillis() / 1000 + 1000);

            // 验证 Token 已失效
            boolean isValid = tokenManager.validateToken(token.getAccessToken());
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("失效空用户 ID 应安全处理")
        void should_handle_null_user_id() {
            tokenManager.invalidateUserSessions(null);

            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Token 刷新测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("应成功刷新访问令牌")
        void should_refresh_access_token() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken originalToken = tokenManager.generateToken(authentication);

            AuthenticationToken refreshedToken = tokenManager.refreshToken(originalToken.getRefreshToken());

            assertThat(refreshedToken).isNotNull();
            assertThat(refreshedToken.getAccessToken()).isNotBlank();
            assertThat(refreshedToken.getAccessToken()).isNotEqualTo(originalToken.getAccessToken());
            assertThat(refreshedToken.getRefreshToken()).isEqualTo(originalToken.getRefreshToken());
        }

        @Test
        @DisplayName("使用访问令牌刷新应失败")
        void should_fail_with_access_token() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            // 使用访问令牌尝试刷新
            boolean isValidRefresh = tokenManager.validateRefreshToken(token.getAccessToken());
            assertThat(isValidRefresh).isFalse();
        }
    }

    @Nested
    @DisplayName("Token 解析测试")
    class ParseTokenTests {

        @Test
        @DisplayName("应正确解析 Token 中的用户信息")
        void should_parse_user_details() {
            Authentication authentication = createTestAuthentication();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            Authentication parsed = tokenManager.parseToken(token.getAccessToken());

            assertThat(parsed).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(parsed.getPrincipal()).isInstanceOf(SysUserDetails.class);

            SysUserDetails userDetails = (SysUserDetails) parsed.getPrincipal();
            assertThat(userDetails.getUserId()).isEqualTo(1L);
            assertThat(userDetails.getDeptId()).isEqualTo(100L);
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应正确解析自定义部门数据权限")
        void should_parse_custom_data_scope() {
            Authentication authentication = createTestAuthenticationWithCustomScope();
            AuthenticationToken token = tokenManager.generateToken(authentication);

            Authentication parsed = tokenManager.parseToken(token.getAccessToken());
            SysUserDetails userDetails = (SysUserDetails) parsed.getPrincipal();

            List<RoleDataScope> dataScopes = userDetails.getDataScopes();
            assertThat(dataScopes).hasSize(1);
            assertThat(dataScopes.get(0).getCustomDeptIds()).containsExactly(10L, 20L, 30L);
        }
    }

    // ========== 测试数据构建方法 ==========

    private SecurityProperties createSecurityProperties() {
        SecurityProperties properties = new SecurityProperties();
        SecurityProperties.SessionConfig sessionConfig = new SecurityProperties.SessionConfig();
        sessionConfig.setType("jwt");
        sessionConfig.setAccessTokenTimeToLive(ACCESS_TOKEN_TTL);
        sessionConfig.setRefreshTokenTimeToLive(REFRESH_TOKEN_TTL);

        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setSecretKey(TEST_SECRET_KEY);
        sessionConfig.setJwt(jwtConfig);

        properties.setSession(sessionConfig);
        properties.setIgnoreUrls(new String[]{"/api/v1/auth/login/**"});
        properties.setUnsecuredUrls(new String[]{"/doc.html"});
        return properties;
    }

    private Authentication createTestAuthentication() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(1L);
        userDetails.setUsername("testuser");
        userDetails.setDeptId(100L);
        userDetails.setEnabled(true);
        userDetails.setDataScopes(List.of(
                new RoleDataScope("ADMIN", 1, null),  // 全部数据权限
                new RoleDataScope("USER", 4, null)    // 本人数据权限
        ));
        userDetails.setAuthorities(Set.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
        ));

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private Authentication createTestAuthenticationWithCustomScope() {
        SysUserDetails userDetails = new SysUserDetails();
        userDetails.setUserId(2L);
        userDetails.setUsername("customuser");
        userDetails.setDeptId(200L);
        userDetails.setEnabled(true);
        userDetails.setDataScopes(List.of(
                new RoleDataScope("CUSTOM_ROLE", 5, List.of(10L, 20L, 30L))  // 自定义部门权限
        ));
        userDetails.setAuthorities(Set.of(
                new SimpleGrantedAuthority("ROLE_CUSTOM")
        ));

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}
