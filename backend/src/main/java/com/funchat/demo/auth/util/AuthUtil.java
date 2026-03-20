package com.funchat.demo.auth.util;

import org.springframework.util.StringUtils;

import java.util.Optional;

public class AuthUtil {
    public static Optional<String> resolveToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ") && token.length() > 7) {
            return Optional.of(token.substring(7));
        }
        return Optional.empty();
    }
}
