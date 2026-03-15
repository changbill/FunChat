package com.funchat.demo.chat.controller;

import com.funchat.demo.chat.service.ChatService;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/chat/messages/{roomId}")
    public ResponseEntity<ResponseDto> getChatMessages(@PathVariable Long roomId) {
        return ResponseUtil.createSuccessResponse(chatService.getChatMessages(roomId));
    }

}