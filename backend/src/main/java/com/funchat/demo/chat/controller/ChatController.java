package com.funchat.demo.chat.controller;

import com.funchat.demo.chat.domain.dto.MessageResponse;
import com.funchat.demo.chat.service.ChatService;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.funchat.demo.global.constants.ChatConstants.MESSAGE_UNITS_TO_RECEIVE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat/messages/{roomId}")
    public ResponseEntity<ResponseDto> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursorId,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseUtil.createSuccessResponse(chatService.getMessages(roomId,cursorId,size));
    }

}