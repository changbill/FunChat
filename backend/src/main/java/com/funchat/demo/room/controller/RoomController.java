package com.funchat.demo.room.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.room.service.RoomService;
import com.funchat.demo.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final MessageBrokerChatService messageBrokerChatService;

    @PostMapping
    public ResponseEntity<ResponseDto> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseUtil.createSuccessResponse(roomService.createRoom(request));
    }

    @GetMapping
    public ResponseEntity<ResponseDto> getAllRooms() {
        return ResponseUtil.createSuccessResponse(roomService.findAllRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDto> getRoom(@PathVariable Long roomId) {
        return ResponseUtil.createSuccessResponse(roomService.findRoom(roomId));
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<ResponseDto> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomUpdateRequest update,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.user().getId();
        RoomResponse response = roomService.updateRoom(roomId, update, userId);
        return ResponseUtil.createSuccessResponse(response);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ResponseDto> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.user().getId();
        roomService.deleteRoom(roomId, userId);
        return ResponseUtil.createSuccessResponse(null);
    }

    @PostMapping("/{roomId}/enter")
    public ResponseEntity<ResponseDto> enterRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.user().getId();
        RoomResponse response = roomService.enterRoom(roomId, userId);
        return ResponseUtil.createSuccessResponse(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ResponseDto> leaveRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.user().getId();
        roomService.leaveRoom(userId);
        return ResponseUtil.createSuccessResponse(null);
    }

    @PatchMapping("/{roomId}/manager")
    public ResponseEntity<ResponseDto> delegateManager(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long newManagerId
    ) {
        Long userId = userDetails.user().getId();
        roomService.delegateManager(roomId, userId, newManagerId);
        return ResponseUtil.createSuccessResponse(null);
    }
}