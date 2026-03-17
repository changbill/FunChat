package com.funchat.demo.room.domain.dto;

import com.funchat.demo.room.domain.RoomUser;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ParticipantResponse(
        Long userId,
        String nickname,
        LocalDateTime joinedAt
) {
    public static ParticipantResponse from(RoomUser roomUser) {
        return ParticipantResponse.builder()
                .userId(roomUser.getUser().getId())
                .joinedAt(roomUser.getJoinedAt())
                .nickname(roomUser.getUser().getNickname())
                .build();
    }
}
