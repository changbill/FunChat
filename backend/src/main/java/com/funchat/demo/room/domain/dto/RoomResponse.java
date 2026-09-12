package com.funchat.demo.room.domain.dto;

import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomType;
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
        LocalDateTime createdAt,
        RoomType roomType
) {
    public static RoomResponse from(Room room, long currentCount) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .roomType(room.getRoomType())
                .maxMembers(room.getMaxMembers())
                .currentMembers(Math.toIntExact(currentCount))
                .managerNickname(room.getManager() != null ? room.getManager().getNickname() : DEFAULT_NICKNAME)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
