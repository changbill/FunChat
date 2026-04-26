package com.funchat.demo.user.controller;

import com.funchat.demo.auth.domain.dto.TokenResponse;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import com.funchat.demo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 시 공통 응답 포맷으로 null body를 반환한다")
    void signUp_ReturnsCommonResponse() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password",
                                  "nickname": "tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body").doesNotExist());

        verify(userService).signUp(new SignUpRequest("user@test.com", "password", "tester"));
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 응답을 공통 응답 body에 담는다")
    void login_ReturnsTokenResponse() throws Exception {
        when(userService.login(new LoginRequest("user@test.com", "password")))
                .thenReturn(TokenResponse.loginOf("access-token", "refresh-token", "tester"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.accessToken").value("access-token"))
                .andExpect(jsonPath("$.body.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.body.nickname").value("tester"));
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 새 토큰을 공통 응답 body에 담는다")
    void reissue_ReturnsTokenResponse() throws Exception {
        when(userService.reissue("Bearer refresh-token"))
                .thenReturn(TokenResponse.reissueOf("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/reissue")
                        .header("Authorization-Refresh", "Bearer refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.body.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.body.nickname").doesNotExist());

        verify(userService).reissue("Bearer refresh-token");
    }

    @Test
    @DisplayName("로그아웃 성공 시 공통 응답 포맷으로 null body를 반환한다")
    void logout_ReturnsCommonResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body").doesNotExist());

        verify(userService).logout("Bearer access-token");
    }
}
