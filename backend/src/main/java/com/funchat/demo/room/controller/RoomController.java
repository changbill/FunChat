package com.funchat.demo.room.controller;

import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.room.service.RoomService;
import com.funchat.demo.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ResponseDto> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseUtil.createSuccessResponse(response);
    }

    @GetMapping
    public ResponseEntity<ResponseDto> getAllRooms() {
        List<RoomResponse> response = roomService.findAllRooms();
        return ResponseUtil.createSuccessResponse(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDto> getRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.findRoomById(roomId);
        return ResponseUtil.createSuccessResponse(response);
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<ResponseDto> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomUpdateRequest update) {
        RoomResponse response = roomService.updateRoom(roomId, update);
        return ResponseUtil.createSuccessResponse(response);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<ResponseDto> deleteRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        roomService.deleteRoom(roomId, userId);
        return ResponseUtil.createSuccessResponse(null);
    }
}