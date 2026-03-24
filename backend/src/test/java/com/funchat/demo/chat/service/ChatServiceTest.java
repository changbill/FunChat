package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.ChatMessage;
import com.funchat.demo.chat.domain.MessageRepository;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.ChatHistoryResponse;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    private Map<String, String> messageMap;

    @BeforeEach
    void setUp() {
        messageMap = Map.of(
                "roomId", "1",
                "senderId", "100",
                "senderNickname", "테스트유저",
                "message", "안녕하세요",
                "messageType", "TEXT"
        );
    }

    @Nested
    @DisplayName("몽고디비에 메시지 저장")
    class SaveMessage {
        @Test
        @DisplayName("성공")
        void success() {
            chatService.saveMessageToMongo(messageMap);
            verify(messageRepository, times(1)).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("roomId 형식이 숫자가 아니면 BusinessException이 발생한다")
        void saveMessage_Fail_InvalidRoomId() {
            // given
            Map<String, String> invalidMap = Map.of("roomId", "abc");

            // when & then
            assertThatThrownBy(() -> chatService.saveMessageToMongo(invalidMap))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND)
                    .hasMessage("roomId 형식이 잘못되었습니다.");
        }

        @Test
        @DisplayName("senderId 형식이 숫자가 아니면 BusinessException이 발생한다")
        void saveMessage_Fail_InvalidSenderId() {
            // given
            Map<String, String> invalidMap = Map.of("roomId", "1", "senderId", "abc");

            // when & then
            assertThatThrownBy(() -> chatService.saveMessageToMongo(invalidMap))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
                    .hasMessage("senderId 형식이 잘못되었습니다.");
        }
    }

    @Nested
    @DisplayName("getMessages (커서 기반 페이징) 테스트")
    class GetMessages {

        @Test
        @DisplayName("첫 페이지 조회 시 (cursorId 없음) findByRoomId가 호출된다")
        void getMessages_FirstPage() {
            // given
            Long roomId = 1L;
            Integer size = 10;
            ChatMessage message = ChatMessage.createMessage(roomId, 100L, "닉네임", "안녕", MessageType.TEXT);

            // Slice 객체 모킹 (데이터 1개 존재, 다음 페이지 없음)
            Slice<ChatMessage> slice = new SliceImpl<>(List.of(message), PageRequest.of(0, size), false);
            when(messageRepository.findByRoomId(eq(roomId), any(Pageable.class))).thenReturn(slice);

            // when
            ChatHistoryResponse response = chatService.getMessages(roomId, null, size);

            // then
            assertThat(response.messages()).hasSize(1);
            assertThat(response.hasNext()).isFalse();
            verify(messageRepository).findByRoomId(eq(roomId), any(Pageable.class));
        }

        @Test
        @DisplayName("다음 페이지가 있는 경우 마지막 메시지의 ID가 다음 커서가 된다")
        void getMessages_NextCursor() {
            // given
            Long roomId = 1L;
            String cursorId = "current-cursor-id";
            ChatMessage msg1 = createMockMessage("id-1");
            ChatMessage msg2 = createMockMessage("id-2");

            Slice<ChatMessage> slice = new SliceImpl<>(List.of(msg1, msg2), PageRequest.of(0, 2), true);
            when(messageRepository.findByRoomIdAndIdLessThan(eq(roomId), eq(cursorId), any(Pageable.class)))
                    .thenReturn(slice);

            // when
            ChatHistoryResponse response = chatService.getMessages(roomId, cursorId, 2);

            // then
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursorId()).isEqualTo("id-2"); // 리스트의 마지막 요소 ID
            verify(messageRepository).findByRoomIdAndIdLessThan(eq(roomId), eq(cursorId), any(Pageable.class));
        }
    }

    // 테스트용 헬퍼 메서드 (ChatMessage 필드에 직접 접근이 어렵거나 ID 설정이 필요할 때)
    private ChatMessage createMockMessage(String id) {
        ChatMessage message = ChatMessage.createMessage(1L, 100L, "닉네임", "내용", MessageType.TEXT);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}