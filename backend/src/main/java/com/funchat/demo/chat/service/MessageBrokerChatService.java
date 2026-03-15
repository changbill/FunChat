package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageBrokerChatService {

    private final MessageBrokerAdapter messageBrokerAdapter;
    private final SimpMessagingTemplate messagingTemplate;

    // 스프링 생명주기 상 빈 주입을 모두 마친 직후에 실행할 로직
    @PostConstruct
    public void init() {
        // messageHandler: 메시지 받은 후 실행될 콜백 함수
        messageBrokerAdapter.subscribe("chat-stream", "chat-group", message -> {
            String roomId = message.get("roomId");
            messagingTemplate.convertAndSend("/sub/chat/" + roomId, message);
            // 웹소켓으로 /sub/chat/ + roomId를 구독하고 있는 뷰단에 메시지 전송
        });
    }

    public void sendChatMessage(String roomId, String inputMessage) {
        Message message = Message.builder()
                .roomId(Long.parseLong(roomId))
                .senderNickname("User") // 임시 유저 정보
                .message(inputMessage)
                .createdAt(LocalDateTime.now())
                .build();

        messageBrokerAdapter.publish("chat-stream", message);
    }
}
