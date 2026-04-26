package com.funchat.demo.room.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.room.service.RoomService;
import com.funchat.demo.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RoomControllerTest.AuthenticationPrincipalResolverConfig.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("채팅방 생성은 인증 사용자 ID로 서비스를 호출하고 공통 응답을 반환한다")
    void createRoom_UsesAuthenticatedUser() throws Exception {
        RoomResponse response = roomResponse();

        when(roomService.createRoom(new RoomRequest("test room", 20), 1L)).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "test room",
                                  "maxMembers": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.roomId").value(10))
                .andExpect(jsonPath("$.body.title").value("test room"))
                .andExpect(jsonPath("$.body.managerNickname").value("tester"));

        verify(roomService).createRoom(new RoomRequest("test room", 20), 1L);
    }

    @Test
    @DisplayName("채팅방 생성 요청 검증 실패 시 공통 에러 응답을 반환한다")
    void createRoom_InvalidRequest_ReturnsErrorResponse() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "maxMembers": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("채팅방 상세 조회는 공통 응답 body에 방 정보를 담는다")
    void getRoom_ReturnsRoomResponse() throws Exception {
        when(roomService.findRoom(10L)).thenReturn(roomResponse());

        mockMvc.perform(get("/api/rooms/{roomId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.roomId").value(10))
                .andExpect(jsonPath("$.body.title").value("test room"));

        verify(roomService).findRoom(10L);
    }

    @Test
    @DisplayName("채팅방 수정은 인증 사용자 ID로 서비스를 호출하고 공통 응답을 반환한다")
    void updateRoom_UsesAuthenticatedUser() throws Exception {
        RoomUpdateRequest request = new RoomUpdateRequest("new title", 30);
        when(roomService.updateRoom(10L, request, 1L)).thenReturn(roomResponse());

        mockMvc.perform(patch("/api/rooms/{roomId}", 10L)
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "new title",
                                  "maxMembers": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.roomId").value(10));

        verify(roomService).updateRoom(10L, request, 1L);
    }

    @Test
    @DisplayName("채팅방 삭제는 인증 사용자 ID로 서비스를 호출하고 null body를 반환한다")
    void deleteRoom_UsesAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/rooms/{roomId}", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body").doesNotExist());

        verify(roomService).deleteRoom(10L, 1L);
    }

    @Test
    @DisplayName("채팅방 입장은 인증 사용자 ID로 서비스를 호출하고 공통 응답을 반환한다")
    void enterRoom_UsesAuthenticatedUser() throws Exception {
        when(roomService.enterRoom(10L, 1L)).thenReturn(roomResponse());

        mockMvc.perform(post("/api/rooms/{roomId}/enter", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.roomId").value(10));

        verify(roomService).enterRoom(10L, 1L);
    }

    @Test
    @DisplayName("채팅방 퇴장은 인증 사용자 ID로 서비스를 호출하고 null body를 반환한다")
    void leaveRoom_UsesAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/leave", 10L)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body").doesNotExist());

        verify(roomService).leaveRoom(1L);
    }

    @Test
    @DisplayName("매니저 위임은 인증 사용자 ID와 새 매니저 ID로 서비스를 호출한다")
    void delegateManager_UsesAuthenticatedUser() throws Exception {
        mockMvc.perform(patch("/api/rooms/{roomId}/manager", 10L)
                        .with(authentication(authenticatedUser()))
                        .param("newManagerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body").doesNotExist());

        verify(roomService).delegateManager(10L, 1L, 2L);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        CustomUserDetails userDetails = new CustomUserDetails(User.createForTest(1L, "user@test.com", "tester", ""));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private RoomResponse roomResponse() {
        return new RoomResponse(
                10L,
                "test room",
                20,
                1,
                "tester",
                LocalDateTime.of(2026, 4, 25, 12, 0)
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
