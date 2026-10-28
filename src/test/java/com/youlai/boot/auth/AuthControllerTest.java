package com.youlai.boot.auth;

import com.youlai.boot.YouLaiBootApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口的外部环境集成测试，依赖 dev 配置中的数据库与 Redis
 *
 * @author Ray.Hao
 * @since 4.6.0
 */
@SpringBootTest(classes = YouLaiBootApplication.class)
@Tag("external")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    private static String accessToken;

    @Test
    @Order(1)
    @DisplayName("登录成功")
    void loginShouldReturnTokenPair() throws Exception {
        String body = """
                {
                    "username": "admin",
                    "password": "123456"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = root.path("data").path("accessToken").asText();

        assertThat(accessToken).as("登录后应返回 accessToken").isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("密码错误登录失败")
    void wrongPasswordLoginShouldFail() throws Exception {
        String body = """
                {
                    "username": "admin",
                    "password": "wrong_password"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0210"))
                .andExpect(jsonPath("$.msg").value("密码错误"));
    }

    @Test
    @Order(3)
    @DisplayName("空用户名登录失败")
    void emptyUsernameLoginShouldFail() throws Exception {
        String body = """
                {
                    "username": "",
                    "password": "123456"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B0001"));
    }

    @Test
    @Order(4)
    @DisplayName("登录后获取当前用户信息")
    void authenticatedUserShouldGetProfile() throws Exception {
        assertThat(accessToken).as("accessToken 应由登录测试先行填充").isNotBlank();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.username").isString())
                .andExpect(jsonPath("$.data.nickname").isString())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.perms").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("无 Token 请求 /me 返回令牌无效")
    void missingTokenShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0230"))
                .andExpect(jsonPath("$.msg").value("令牌无效或已过期"));
    }

    @Test
    @Order(6)
    @DisplayName("伪造 Token 请求 /me 返回令牌无效")
    void forgedTokenShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid_token_xxx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0230"));
    }

}
