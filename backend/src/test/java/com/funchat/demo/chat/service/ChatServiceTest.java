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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.funchat.demo.global.constants.ChatConstants.MESSAGE_CONTENT;
import static com.funchat.demo.global.constants.ChatConstants.MESSAGE_TYPE;
import static com.funchat.demo.global.constants.ChatConstants.ROOM_ID;
import static com.funchat.demo.global.constants.ChatConstants.SENDER_ID;
import static com.funchat.demo.global.constants.ChatConstants.SENDER_NICKNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {ChatService.class, ChatServiceTest.FakeMessageRepositoryConfig.class})
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
    }

    @Nested
    @DisplayName("몽고디비에 메시지 저장")
    class SaveMessage {

        @Test
        @DisplayName("성공")
        void whenMessageMapIsValid_thenSaveMessage() {
            // given
            Map<String, String> messageMap = Map.of(
                    ROOM_ID, "1",
                    SENDER_ID, "100",
                    SENDER_NICKNAME, "테스트유저",
                    MESSAGE_CONTENT, "안녕하세요",
                    MESSAGE_TYPE, "TEXT"
            );

            // when
            chatService.saveMessageToMongo(messageMap);

            // then
            assertThat(messageRepository.findAll())
                    .singleElement()
                    .satisfies(message -> {
                        assertThat(message.getRoomId()).isEqualTo(1L);
                        assertThat(message.getSenderId()).isEqualTo(100L);
                        assertThat(message.getSenderNickname()).isEqualTo("테스트유저");
                        assertThat(message.getContent()).isEqualTo("안녕하세요");
                        assertThat(message.getType()).isEqualTo(MessageType.TEXT);
                    });
        }

        @Test
        @DisplayName("messageType이 없으면 TEXT 타입으로 저장한다")
        void whenMessageTypeIsMissing_thenSaveAsTextMessage() {
            // given
            Map<String, String> messageMap = Map.of(
                    ROOM_ID, "1",
                    SENDER_ID, "100",
                    SENDER_NICKNAME, "테스트유저",
                    MESSAGE_CONTENT, "안녕하세요"
            );

            // when
            chatService.saveMessageToMongo(messageMap);

            // then
            assertThat(messageRepository.findAll())
                    .singleElement()
                    .extracting(ChatMessage::getType)
                    .isEqualTo(MessageType.TEXT);
        }

        @Test
        @DisplayName("roomId 형식이 숫자가 아니면 실패")
        void whenRoomIdIsNotNumeric_thenThrowException() {
            // given
            Map<String, String> invalidMap = Map.of(
                    ROOM_ID, "abc",
                    SENDER_ID, "100"
            );

            // when & then
            assertThatThrownBy(() -> chatService.saveMessageToMongo(invalidMap))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND)
                    .hasMessage("roomId 형식이 잘못되었습니다.");
        }

        @Test
        @DisplayName("senderId 형식이 숫자가 아니면 실패")
        void whenSenderIdIsNotNumeric_thenThrowException() {
            // given
            Map<String, String> invalidMap = Map.of(
                    ROOM_ID, "1",
                    SENDER_ID, "abc"
            );

            // when & then
            assertThatThrownBy(() -> chatService.saveMessageToMongo(invalidMap))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
                    .hasMessage("senderId 형식이 잘못되었습니다.");
        }
    }

    @Nested
    @DisplayName("채팅 이력 조회")
    class GetMessages {

        @Test
        @DisplayName("첫 페이지는 roomId 기준으로 최신 메시지를 조회한다")
        void whenCursorIsMissing_thenReturnFirstPage() {
            // given
            ChatMessage first = saveMessage(1L, "첫 번째");
            ChatMessage second = saveMessage(1L, "두 번째");
            saveMessage(2L, "다른 방 메시지");

            // when
            ChatHistoryResponse response = chatService.getMessages(1L, null, 10);

            // then
            assertThat(response.messages()).hasSize(2);
            assertThat(response.messages())
                    .extracting("messageId")
                    .containsExactly(second.getId(), first.getId());
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursorId()).isNull();
        }

        @Test
        @DisplayName("다음 페이지가 있으면 마지막 메시지 ID를 nextCursorId로 반환한다")
        void whenMoreMessagesExist_thenReturnNextCursorId() {
            // given
            ChatMessage first = saveMessage(1L, "첫 번째");
            ChatMessage second = saveMessage(1L, "두 번째");
            ChatMessage third = saveMessage(1L, "세 번째");

            // when
            ChatHistoryResponse response = chatService.getMessages(1L, null, 2);

            // then
            assertThat(response.messages())
                    .extracting("messageId")
                    .containsExactly(third.getId(), second.getId());
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursorId()).isEqualTo(second.getId());

            ChatHistoryResponse nextPage = chatService.getMessages(1L, response.nextCursorId(), 2);
            assertThat(nextPage.messages())
                    .extracting("messageId")
                    .containsExactly(first.getId());
            assertThat(nextPage.hasNext()).isFalse();
        }
    }

    private ChatMessage saveMessage(Long roomId, String content) {
        return messageRepository.save(ChatMessage.createMessage(roomId, 100L, "닉네임", content, MessageType.TEXT));
    }

    @TestConfiguration
    static class FakeMessageRepositoryConfig {

        @Bean
        @Primary
        MessageRepository messageRepository() {
            AtomicLong sequence = new AtomicLong();
            List<ChatMessage> messages = new ArrayList<>();

            return (MessageRepository) Proxy.newProxyInstance(
                    MessageRepository.class.getClassLoader(),
                    new Class<?>[]{MessageRepository.class},
                    (proxy, method, args) -> {
                        String methodName = method.getName();

                        if (methodName.equals("save")) {
                            ChatMessage message = (ChatMessage) args[0];
                            if (message.getId() == null) {
                                ReflectionTestUtils.setField(message, "id", String.format("%024d", sequence.incrementAndGet()));
                            }
                            messages.add(message);
                            return message;
                        }

                        if (methodName.equals("deleteAll") && (args == null || args.length == 0)) {
                            messages.clear();
                            return null;
                        }

                        if (methodName.equals("findAll") && (args == null || args.length == 0)) {
                            return List.copyOf(messages);
                        }

                        if (methodName.equals("findByRoomId")) {
                            Long roomId = (Long) args[0];
                            int size = ((org.springframework.data.domain.Pageable) args[1]).getPageSize();
                            return slice(messages.stream()
                                    .filter(message -> message.getRoomId().equals(roomId))
                                    .sorted(Comparator.comparing(ChatMessage::getId).reversed())
                                    .toList(), size);
                        }

                        if (methodName.equals("findByRoomIdAndIdLessThan")) {
                            Long roomId = (Long) args[0];
                            String cursorId = (String) args[1];
                            int size = ((org.springframework.data.domain.Pageable) args[2]).getPageSize();
                            return slice(messages.stream()
                                    .filter(message -> message.getRoomId().equals(roomId))
                                    .filter(message -> message.getId().compareTo(cursorId) < 0)
                                    .sorted(Comparator.comparing(ChatMessage::getId).reversed())
                                    .toList(), size);
                        }

                        if (methodName.equals("toString")) {
                            return "FakeMessageRepository";
                        }

                        if (methodName.equals("hashCode")) {
                            return System.identityHashCode(proxy);
                        }

                        if (methodName.equals("equals")) {
                            return proxy == args[0];
                        }

                        throw new UnsupportedOperationException("Unsupported repository method: " + method);
                    }
            );
        }

        private static Slice<ChatMessage> slice(List<ChatMessage> sortedMessages, int size) {
            boolean hasNext = sortedMessages.size() > size;
            List<ChatMessage> content = sortedMessages.stream()
                    .limit(size)
                    .toList();
            return new SliceImpl<>(content, PageRequest.of(0, size), hasNext);
        }
    }
}
