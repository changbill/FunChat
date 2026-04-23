package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.global.constants.SystemConstants;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.funchat.demo.global.constants.ChatConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageBrokerChatService {

    private final ChatPersistBroker persistBroker;
    private final ChatFanoutBroker fanoutBroker;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @PostConstruct
    public void init() {
        // 1) Durable path: persist to Mongo exactly-once-ish
        persistBroker.subscribe(dto -> {
            // Convert to the existing Mongo save map format
            chatService.saveMessageToMongo(java.util.Map.of(
                    ROOM_ID, String.valueOf(dto.roomId()),
                    SENDER_ID, String.valueOf(dto.senderId()),
                    SENDER_NICKNAME, dto.senderNickname(),
                    MESSAGE_CONTENT, dto.content(),
                    MESSAGE_TYPE, dto.type().name(),
                    "createdAt", dto.createdAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
        });

        // 2) Best-effort fanout: every instance pushes to its local websocket sessions
        fanoutBroker.subscribe(dto -> {
            String roomId = String.valueOf(dto.roomId());
            messagingTemplate.convertAndSend(SUBSCRIPTION_URL + roomId, dto);
        });
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

        persistBroker.publish(message);
        fanoutBroker.publish(message);
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

        persistBroker.publish(message);
        fanoutBroker.publish(message);
    }
}
