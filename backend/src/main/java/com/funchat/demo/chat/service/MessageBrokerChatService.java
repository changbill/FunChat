package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.chat.service.redis.RedisMessageBrokerAdapter;
import com.funchat.demo.global.constants.SystemConstants;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.funchat.demo.global.constants.ChatConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageBrokerChatService {

    private final RedisMessageBrokerAdapter messageBrokerAdapter;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @PostConstruct
    public void init() {
        // 메시지 브로커에서 STREAM_TOPIC, CONSUMER_GROUP를 기준으로 메시지 구독
        messageBrokerAdapter.subscribe(STREAM_TOPIC, CONSUMER_GROUP, this::dispatchStreamMessageToClients);
    }

    private void dispatchStreamMessageToClients(Map<String, String> message) {
        String roomId = message.get(ROOM_ID);
        messagingTemplate.convertAndSend(SUBSCRIPTION_URL + roomId, message);
        chatService.saveMessageToMongo(message);
    }

    public void sendChatMessageToRedisStreams(Long roomId, Long senderId, String nickname, String inputMessage) {
        RedisStreamsMessageDto message = RedisStreamsMessageDto.builder()
                .roomId(roomId)
                .senderId(senderId)     // 인증된 유저 ID 주입
                .senderNickname(nickname) // 인증된 유저 닉네임 주입
                .content(inputMessage)
                .type(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();

        messageBrokerAdapter.publish(STREAM_TOPIC, message);
    }

    public void sendNoticeToRedisStreams(Long roomId, String nickname, MessageType type) {
        String content = switch (type) {
            case MessageType.JOIN -> nickname + SystemConstants.ENTER_MENTION;
            case MessageType.LEAVE -> nickname + SystemConstants.EXIT_MENTION;
            case MessageType.DELEGATE -> nickname + SystemConstants.DELEGATION_MENTION;
            case MessageType.BAN -> nickname + SystemConstants.BAN_MENTION;
            default -> throw new BusinessException(ErrorCode.MESSAGE_SEND_FAILED, "Invalid message type");
        };

        RedisStreamsMessageDto message = RedisStreamsMessageDto.builder()
                .roomId(roomId)
                .senderId(SystemConstants.USER_ID) // 시스템 계정 ID
                .senderNickname(SystemConstants.USER_NICKNAME)
                .content(content)
                .type(type) // JOIN 또는 LEAVE
                .createdAt(LocalDateTime.now())
                .build();

        messageBrokerAdapter.publish(STREAM_TOPIC, message);
    }
}
