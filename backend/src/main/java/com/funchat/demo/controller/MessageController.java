package com.funchat.demo.controller;

import com.funchat.demo.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/message") // 클라이언트가 "/app/chat/message"로 보낼 때
    public void message(ChatMessageDto message) {

        // 1. 입장 메시지 처리
        if (ChatMessageDto.MessageType.ENTER.equals(message.getType())) {
            message.setMessage(message.getSender() + "님이 입장하셨습니다.");
        }

        // 2. 메시지 전송
        // "/topic/group/{roomId}"를 구독 중인 모든 사용자에게 메시지 전달
        messagingTemplate.convertAndSend("/topic/group/" + message.getRoomId(), message);
    }
}