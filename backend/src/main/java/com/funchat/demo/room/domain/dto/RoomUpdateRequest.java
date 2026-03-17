package com.funchat.demo.room.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Builder
public record RoomUpdateRequest(
        @NotBlank(message = "방 제목은 비어있을 수 없습니다.")
        String title,
        @Min(value = 2, message = "최소 인원은 2명 이상이어야 합니다.")
        Integer maxMembers,
        @NotNull(message = "수정 요청자 ID는 필수입니다.")
        Long userId
) {
}
