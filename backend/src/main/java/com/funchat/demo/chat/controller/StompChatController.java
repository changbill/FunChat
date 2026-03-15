package com.funchat.demo.chat.controller;

import com.funchat.demo.chat.domain.dto.ChatMessageRequest;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {
    private final MessageBrokerChatService messageBrokerChatService;

    @MessageMapping("/send/{roomId}")
    public void sendChatMessage(@DestinationVariable(value = "roomId") String roomId, @Payload ChatMessageRequest request) {
        log.info(">>>> [STOMP Controller] Received message for room {}: {}", roomId, request);
        messageBrokerChatService.sendChatMessage(roomId, request.message());
        log.info(">>>> [STOMP Controller] Handled message for room {}", roomId);
    }
}