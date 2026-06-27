package com.funchat.demo.video.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.video.domain.VideoSessionStatus;
import com.funchat.demo.video.domain.dto.VideoSessionResponse;
import com.funchat.demo.video.domain.dto.VideoTokenResponse;
import com.funchat.demo.video.service.VideoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VideoControllerTest.AuthenticationPrincipalResolverConfig.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VideoService videoService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("영상 세션 시작은 인증 사용자 ID로 서비스를 호출하고 공통 응답을 반환한다")
    void startSession_UsesAuthenticatedUser() throws Exception {
        when(videoService.startSession(10L, 1L)).thenReturn(videoSessionResponse());

        mockMvc.perform(post("/api/rooms/{roomId}/video/sessions", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.sessionId").value(100))
                .andExpect(jsonPath("$.body.livekitRoomName").value("funchat-room-10"))
                .andExpect(jsonPath("$.body.status").value("ACTIVE"));

        verify(videoService).startSession(10L, 1L);
    }

    @Test
    @DisplayName("활성 영상 세션 조회는 인증 사용자 ID로 서비스를 호출한다")
    void getActiveSession_UsesAuthenticatedUser() throws Exception {
        when(videoService.findActiveSession(10L, 1L)).thenReturn(videoSessionResponse());

        mockMvc.perform(get("/api/rooms/{roomId}/video/session", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.sessionId").value(100));

        verify(videoService).findActiveSession(10L, 1L);
    }

    @Test
    @DisplayName("영상 참가 토큰 발급은 인증 사용자 ID로 서비스를 호출하고 토큰 응답을 반환한다")
    void issueJoinToken_UsesAuthenticatedUser() throws Exception {
        when(videoService.issueJoinToken(10L, 1L)).thenReturn(videoTokenResponse());

        mockMvc.perform(post("/api/rooms/{roomId}/video/token", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.livekitUrl").value("http://localhost:7880"))
                .andExpect(jsonPath("$.body.identity").value("user-1"))
                .andExpect(jsonPath("$.body.token").value("livekit-token"));

        verify(videoService).issueJoinToken(10L, 1L);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        CustomUserDetails userDetails = new CustomUserDetails(User.createForTest(1L, "user@test.com", "tester", ""));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private VideoSessionResponse videoSessionResponse() {
        return new VideoSessionResponse(
                100L,
                10L,
                "funchat-room-10",
                VideoSessionStatus.ACTIVE,
                LocalDateTime.of(2026, 5, 12, 12, 0),
                null
        );
    }

    private VideoTokenResponse videoTokenResponse() {
        return new VideoTokenResponse(
                100L,
                10L,
                "http://localhost:7880",
                "funchat-room-10",
                "user-1",
                "tester",
                "livekit-token",
                LocalDateTime.of(2026, 5, 12, 13, 0)
        );
    }

    @TestConfiguration
    static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(0, new AuthenticationPrincipalArgumentResolver());
        }
    }
}
