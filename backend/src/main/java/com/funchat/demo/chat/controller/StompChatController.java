package com.funchat.demo.chat.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.chat.domain.dto.MessageRequest;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {
    private final MessageBrokerChatService messageBrokerChatService;

    /**
     * STOMP로 메시지를 보낼 때 헤더에 roomId를 반드시 포함
     *
     * @param request
     * @param message
     */
    @MessageMapping("/chat/message")
    public void message(MessageRequest request, Message<?> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        User user = extractUser(accessor.getUser());

        messageBrokerChatService.sendChatMessageToRedisStreams(
                request.roomId(),
                user.getId(),
                user.getNickname(),
                request.content()
        );
    }

    private User extractUser(Principal principal) {
        return Optional.ofNullable(principal)
                .map(p -> (UsernamePasswordAuthenticationToken) p)
                .map(auth -> (CustomUserDetails) auth.getPrincipal())
                .map(CustomUserDetails::user)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}