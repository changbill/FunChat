package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.dto.MessageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    MessageBrokerChatService messageBrokerChatService;

    public List<MessageResponse> getChatMessages (Long roomId) {
        return null;
    }
}
