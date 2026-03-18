package com.funchat.demo.user.domain.dto;

import lombok.Builder;

@Builder
public record SignUpRequest(
        String email,
        String password,
        String nickname
) {}
