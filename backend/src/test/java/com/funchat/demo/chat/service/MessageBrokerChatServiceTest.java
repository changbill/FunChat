package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.global.constants.SystemConstants;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageBrokerChatServiceTest {

    private RecordingBroker persistBroker;
    private RecordingBroker fanoutBroker;
    private MessageBrokerChatService messageBrokerChatService;

    @BeforeEach
    void setUp() {
        persistBroker = new RecordingBroker();
        fanoutBroker = new RecordingBroker();
        messageBrokerChatService = new MessageBrokerChatService(
                persistBroker,
                fanoutBroker,
                nullSimpMessagingTemplate(),
                null
        );
    }

    @Nested
    @DisplayName("일반 채팅 메시지 발행")
    class SendChatMessage {

        @Test
        @DisplayName("성공")
        void whenChatMessageIsSent_thenPublishTextMessage() {
            // given
            Long roomId = 1L;
            Long senderId = 2L;

            // when
            messageBrokerChatService.sendChatMessageToRedisStreams(roomId, senderId, "tester", "hello");

            // then
            RedisStreamsMessageDto message = persistBroker.singlePublishedMessage();
            assertThat(message.roomId()).isEqualTo(roomId);
            assertThat(message.senderId()).isEqualTo(senderId);
            assertThat(message.senderNickname()).isEqualTo("tester");
            assertThat(message.content()).isEqualTo("hello");
            assertThat(message.type()).isEqualTo(MessageType.TEXT);
            assertThat(fanoutBroker.publishedMessages()).containsExactly(message);
        }

        @Test
        @DisplayName("저장 경로 발행 실패 시 메시지 전송 실패로 처리하고 팬아웃하지 않는다")
        void whenPersistPublishFails_thenThrowExceptionAndSkipFanout() {
            // given
            persistBroker.failPublish();

            // when & then
            assertThatThrownBy(() -> messageBrokerChatService.sendChatMessageToRedisStreams(1L, 2L, "tester", "hello"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_SEND_FAILED);
            assertThat(fanoutBroker.publishedMessages()).isEmpty();
        }

        @Test
        @DisplayName("팬아웃 발행 실패는 저장 성공을 되돌리지 않는다")
        void whenFanoutPublishFails_thenKeepPersistSuccess() {
            // given
            fanoutBroker.failPublish();

            // when
            messageBrokerChatService.sendChatMessageToRedisStreams(1L, 2L, "tester", "hello");

            // then
            assertThat(persistBroker.publishedMessages()).hasSize(1);
            assertThat(fanoutBroker.publishedMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("시스템 알림 발행")
    class SendNotice {

        @Test
        @DisplayName("성공")
        void whenNoticeIsSent_thenPublishSystemMessage() {
            // when
            messageBrokerChatService.sendNoticeToRedisStreams(1L, "tester", MessageType.JOIN);

            // then
            RedisStreamsMessageDto message = persistBroker.singlePublishedMessage();
            assertThat(message.roomId()).isEqualTo(1L);
            assertThat(message.senderId()).isEqualTo(SystemConstants.USER_ID);
            assertThat(message.senderNickname()).isEqualTo(SystemConstants.USER_NICKNAME);
            assertThat(message.content()).isEqualTo("tester" + SystemConstants.ENTER_MENTION);
            assertThat(message.type()).isEqualTo(MessageType.JOIN);
            assertThat(fanoutBroker.publishedMessages()).containsExactly(message);
        }

        @Test
        @DisplayName("TEXT 타입은 시스템 알림 타입으로 사용할 수 없다")
        void whenNoticeTypeIsText_thenThrowException() {
            // when & then
            assertThatThrownBy(() -> messageBrokerChatService.sendNoticeToRedisStreams(1L, "tester", MessageType.TEXT))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_SEND_FAILED);
            assertThat(persistBroker.publishedMessages()).isEmpty();
            assertThat(fanoutBroker.publishedMessages()).isEmpty();
        }
    }

    private SimpMessagingTemplate nullSimpMessagingTemplate() {
        return null;
    }

    private static class RecordingBroker implements ChatPersistBroker, ChatFanoutBroker {
        private final List<RedisStreamsMessageDto> publishedMessages = new ArrayList<>();
        private boolean failPublish;

        @Override
        public void publish(RedisStreamsMessageDto message) {
            if (failPublish) {
                throw new RuntimeException("publish failed");
            }

            publishedMessages.add(message);
        }

        @Override
        public void subscribe(Consumer<RedisStreamsMessageDto> handler) {
        }

        void failPublish() {
            this.failPublish = true;
        }

        List<RedisStreamsMessageDto> publishedMessages() {
            return publishedMessages;
        }

        RedisStreamsMessageDto singlePublishedMessage() {
            assertThat(publishedMessages).hasSize(1);
            return publishedMessages.getFirst();
        }
    }
}
