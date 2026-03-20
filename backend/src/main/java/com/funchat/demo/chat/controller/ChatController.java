package com.funchat.demo.chat.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.service.ChatService;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.room.service.RoomService;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final RedisService redisService;
    private final RoomService roomService;
    private final MessageBrokerChatService messageBrokerChatService;

    @GetMapping("/chat/messages/{roomId}")
    public ResponseEntity<ResponseDto> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursorId,
            @RequestParam(defaultValue = "100") Integer size
    ) {
        return ResponseUtil.createSuccessResponse(chatService.getMessages(roomId,cursorId,size));
    }

    @PostMapping("/rooms/{roomId}/enter")
    public ResponseEntity<ResponseDto> enterRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        redisService.saveUserCurrentRoom(userDetails.user().getId(), roomId);
        messageBrokerChatService.sendNoticeToRedisStreams(roomId, userDetails.user().getNickname(), MessageType.JOIN);
        return ResponseUtil.createSuccessResponse(roomService.findRoomById(roomId));
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ResponseDto> leaveRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        messageBrokerChatService.sendNoticeToRedisStreams(roomId, userDetails.user().getNickname(), MessageType.LEAVE);
        redisService.deleteUserCurrentRoom(userDetails.user().getId());
        return ResponseUtil.createSuccessResponse(null);
    }
}