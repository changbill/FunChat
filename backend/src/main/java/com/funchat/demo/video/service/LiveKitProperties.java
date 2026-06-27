package com.funchat.demo.video.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "livekit")
public record LiveKitProperties(
        String apiKey,
        String apiSecret,
        String url,
        long tokenTtlSeconds
) {
}
