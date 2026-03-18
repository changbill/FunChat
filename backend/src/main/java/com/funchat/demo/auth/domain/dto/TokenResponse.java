package com.funchat.demo.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nickname
) {
    public static TokenResponse loginOf(String at, String rt, String nickname) {
        return new TokenResponse(at, rt, nickname);
    }

    public static TokenResponse reissueOf(String at, String rt) {
        return new TokenResponse(at, rt, null);
    }
}
