package com.funchat.demo.video.domain.dto;

import com.funchat.demo.video.domain.VideoSession;
import com.funchat.demo.video.domain.VideoSessionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VideoSessionResponse(
        Long sessionId,
        Long roomId,
        String livekitRoomName,
        VideoSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static VideoSessionResponse from(VideoSession session) {
        return VideoSessionResponse.builder()
                .sessionId(session.getId())
                .roomId(session.getRoom().getId())
                .livekitRoomName(session.getLivekitRoomName())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
