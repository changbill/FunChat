package com.funchat.demo.chat.controller;

import com.funchat.demo.chat.service.ChatService;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat/messages/{roomId}")
    public ResponseEntity<ResponseDto> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursorId,
            @RequestParam(defaultValue = "100") Integer size
    ) {
        return ResponseUtil.createSuccessResponse(chatService.getMessages(roomId,cursorId,size));
    }


}