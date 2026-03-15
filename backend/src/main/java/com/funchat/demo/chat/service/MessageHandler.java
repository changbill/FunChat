package com.funchat.demo.chat.service;

import java.util.Map;

@FunctionalInterface
public interface MessageHandler {
    void handle(Map<String, String> message);
}
