package com.funchat.demo.chat.controller;

import com.funchat.demo.chat.domain.dto.ChatHistoryResponse;
import com.funchat.demo.chat.domain.dto.MessageResponse;
import com.funchat.demo.chat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    @DisplayName("채팅 이력 조회는 커서 응답을 공통 응답 body에 담는다")
    void getChatMessages_ReturnsHistoryResponse() throws Exception {
        MessageResponse message = new MessageResponse(
                "message-1",
                1L,
                2L,
                "tester",
                "hello",
                LocalDateTime.of(2026, 4, 25, 12, 0)
        );
        ChatHistoryResponse response = ChatHistoryResponse.of(List.of(message), "message-1", true);
        when(chatService.getMessages(1L, null, 10)).thenReturn(response);

        mockMvc.perform(get("/api/chat/messages/{roomId}", 1L)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.messages[0].messageId").value("message-1"))
                .andExpect(jsonPath("$.body.messages[0].content").value("hello"))
                .andExpect(jsonPath("$.body.nextCursorId").value("message-1"))
                .andExpect(jsonPath("$.body.hasNext").value(true));

        verify(chatService).getMessages(1L, null, 10);
    }

    @Test
    @DisplayName("채팅 이력 조회는 cursorId와 기본 size를 서비스에 전달한다")
    void getChatMessages_UsesCursorAndDefaultSize() throws Exception {
        ChatHistoryResponse response = ChatHistoryResponse.of(List.of(), null, false);
        when(chatService.getMessages(1L, "message-1", 100)).thenReturn(response);

        mockMvc.perform(get("/api/chat/messages/{roomId}", 1L)
                        .param("cursorId", "message-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.hasNext").value(false));

        verify(chatService).getMessages(1L, "message-1", 100);
    }
}
