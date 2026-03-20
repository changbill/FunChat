package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.ChatMessage;
import com.funchat.demo.chat.domain.MessageRepository;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.ChatHistoryResponse;
import com.funchat.demo.chat.domain.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.funchat.demo.global.constants.ChatConstants.*;
import static com.funchat.demo.global.constants.ChatConstants.MESSAGE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;

    public void saveMessageToMongo(Map<String, String> messageMap) {
        ChatMessage message = ChatMessage.builder()
                .roomId(Long.parseLong(messageMap.get(ROOM_ID)))
                .senderId(Long.parseLong(messageMap.get(SENDER_ID)))
                .senderNickname(messageMap.get(SENDER_NICKNAME))
                .content(messageMap.get(MESSAGE))
                .type(MessageType.valueOf(messageMap.get(MESSAGE_TYPE)))
                .build();

        messageRepository.save(message);
    }

    public ChatHistoryResponse getMessages(Long roomId, String cursorId, Integer size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "_id"));

        Slice<ChatMessage> mongoChatMessages;
        if (cursorId == null) {
            mongoChatMessages = messageRepository.findByRoomId(roomId, pageable);
        } else {
            mongoChatMessages = messageRepository.findByRoomIdAndIdLessThan(roomId, cursorId, pageable);
        }

        String nextCursorId =
                mongoChatMessages.hasNext() ?
                mongoChatMessages.getContent().get(mongoChatMessages.getNumberOfElements() - 1).getId() :
                null;

        return ChatHistoryResponse.of(
                mongoChatMessages.getContent().stream()
                        .map(MessageResponse::from)
                        .toList(),
                nextCursorId,
                mongoChatMessages.hasNext()
        );
    }
}
