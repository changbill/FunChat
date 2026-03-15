package com.funchat.demo.room.controller;

import com.funchat.demo.chat.domain.dto.RoomRequest;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.room.service.RoomService;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    public ResponseEntity<ResponseDto> getRooms() {
        return ResponseUtil.createSuccessResponse(roomService.getChatRooms());
    }

    @PostMapping("/room")
    public ResponseEntity<ResponseDto> createRoom(@RequestBody RoomRequest roomRequest) {
        return ResponseUtil.createSuccessResponse(roomService.createChatRoom(roomRequest));
    }
}