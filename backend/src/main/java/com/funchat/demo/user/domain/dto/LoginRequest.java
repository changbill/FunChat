package com.funchat.demo.user.domain.dto;

public record LoginRequest(
        String email,
        String password
) {}