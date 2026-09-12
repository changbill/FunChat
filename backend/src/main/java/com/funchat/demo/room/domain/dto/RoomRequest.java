package com.funchat.demo.room.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.funchat.demo.room.domain.RoomType;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RoomRequest(
        @NotBlank(message = "방 제목은 필수입니다.")
        String title,
        @Min(value = 2, message = "최소 인원은 2명 이상이어야 합니다.")
        @NotNull(message = "최대 인원은 필수입니다.")
        Integer maxMembers,
        RoomType roomType
) {
    public RoomRequest {
        roomType = roomType == null ? RoomType.TEXT : roomType;
    }

    public RoomRequest(String title, Integer maxMembers) {
        this(title, maxMembers, RoomType.TEXT);
    }
}
