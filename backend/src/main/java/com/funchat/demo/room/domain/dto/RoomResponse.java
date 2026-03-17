package com.funchat.demo.room.domain.dto;

import com.funchat.demo.room.domain.Room;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.funchat.demo.global.constants.UserConstants.DEFAULT_NICKNAME;

@Builder
public record RoomResponse(
        Long roomId,
        String title,
        Integer maxMembers,
        Integer currentMembers,
        String managerNickname,
        LocalDateTime createdAt
) {
    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .maxMembers(room.getMaxMembers())
                .currentMembers(room.getRoomUsers().size())
                .managerNickname(room.getManager() != null ? room.getManager().getNickname() : DEFAULT_NICKNAME)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
