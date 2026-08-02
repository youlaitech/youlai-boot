package com.youlai.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证 + 当前用户接口单元测试
 *
 * @author Ray.Hao
 * @since 4.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;

    @Test
    @Order(1)
    @DisplayName("登录成功")
    void loginSuccess() throws Exception {
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

        assertNotNull(accessToken, "登录后应返回 accessToken");
        assertFalse(accessToken.isBlank(), "accessToken 不能为空");
    }

    @Test
    @Order(2)
    @DisplayName("密码错误登录失败")
    void loginWithWrongPassword() throws Exception {
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
    void loginWithEmptyUsername() throws Exception {
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
    void getCurrentUserWithToken() throws Exception {
        assertNotNull(accessToken, "accessToken 应由登录测试先行填充");

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
    void getCurrentUserWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0230"))
                .andExpect(jsonPath("$.msg").value("令牌无效或已过期"));
    }

    @Test
    @Order(6)
    @DisplayName("伪造 Token 请求 /me 返回令牌无效")
    void getCurrentUserWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid_token_xxx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0230"));
    }

}
