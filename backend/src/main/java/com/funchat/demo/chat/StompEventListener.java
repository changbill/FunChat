package com.funchat.demo.chat;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompEventListener {

    private final MessageBrokerChatService messageBrokerChatService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("새로운 세션 연결: {}", headerAccessor.getSessionId());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");
        Principal principal = headerAccessor.getUser();

        if (roomId != null && principal != null) {
            var auth = (UsernamePasswordAuthenticationToken) principal;
            var userDetails = (CustomUserDetails) auth.getPrincipal();
            String nickname = userDetails.user().getNickname();

            messageBrokerChatService.sendNoticeToRedisStreams(roomId, nickname, MessageType.LEAVE);
            log.info("[퇴장] User {} left room {}", nickname, roomId);
        }
    }
}