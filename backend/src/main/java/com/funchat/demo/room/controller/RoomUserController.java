package com.funchat.demo.room.controller;

import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.room.domain.dto.ParticipantResponse;
import com.funchat.demo.room.service.RoomUserService;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/users")
@RequiredArgsConstructor
public class RoomUserController {

    private final RoomUserService roomUserService;

    @PostMapping
    public ResponseEntity<ResponseDto> joinRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        roomUserService.joinRoom(roomId, userId);
        return ResponseUtil.createSuccessResponse(null);
    }

    @DeleteMapping
    public ResponseEntity<ResponseDto> leaveRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        roomUserService.leaveRoom(roomId, userId);
        return ResponseUtil.createSuccessResponse(null);
    }

    @GetMapping
    public ResponseEntity<ResponseDto> getParticipants(@PathVariable Long roomId) {
        List<ParticipantResponse> response = roomUserService.getParticipants(roomId);
        return ResponseUtil.createSuccessResponse(response);
    }
}