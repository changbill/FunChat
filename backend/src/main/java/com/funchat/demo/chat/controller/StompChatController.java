package com.funchat.demo.chat.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.MessageRequest;
import com.funchat.demo.chat.service.ChatService;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {
    private final MessageBrokerChatService messageBrokerChatService;
    private final ChatService chatService;

    /**
     * STOMP로 메시지를 보낼 때 헤더에 roomId를 반드시 포함
     *
     * @param request
     * @param principal
     */
    @MessageMapping("/chat/message")
    public void message(MessageRequest request, Principal principal) {
        User user = extractUser(principal);

        messageBrokerChatService.sendChatMessageToRedisStreams(
                request.roomId(),
                user.getId(),
                user.getNickname(),
                request.content()
        );
    }

    private User extractUser(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.user();
    }
}