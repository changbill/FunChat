package com.funchat.demo.chat.domain.dto;

import lombok.Builder;

@Builder
public record ChatRoomResponse(Long id, String name, Long participantCount) {}
