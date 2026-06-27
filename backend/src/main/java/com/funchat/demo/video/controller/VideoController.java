package com.funchat.demo.video.controller;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.util.ResponseUtil;
import com.funchat.demo.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/sessions")
    public ResponseEntity<ResponseDto> startSession(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.user().getId();
        return ResponseUtil.createSuccessResponse(videoService.startSession(roomId, userId));
    }

    @GetMapping("/session")
    public ResponseEntity<ResponseDto> getActiveSession(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.user().getId();
        return ResponseUtil.createSuccessResponse(videoService.findActiveSession(roomId, userId));
    }

    @PostMapping("/token")
    public ResponseEntity<ResponseDto> issueJoinToken(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.user().getId();
        return ResponseUtil.createSuccessResponse(videoService.issueJoinToken(roomId, userId));
    }
}
