package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.Message;
import com.funchat.demo.chat.domain.MessageRepository;
import com.funchat.demo.chat.domain.dto.ChatHistoryResponse;
import com.funchat.demo.chat.domain.dto.MessageResponse;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.funchat.demo.global.constants.ChatConstants.ROOM_ID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final MessageBrokerChatService messageBrokerChatService;

    public ChatHistoryResponse getMessages(Long roomId, String cursorId, Integer size) {
        List<MessageResponse> messages = messageBrokerChatService.getMessage(roomId,cursorId,size);

        boolean hasNext;
        if (messages.size() < size) {
            int remainingNeeded = size - messages.size();
            hasNext = addMongoMessages(roomId, cursorId, remainingNeeded, messages);
        } else {
            hasNext = true;
        }

        String nextCursorId = messages.isEmpty() ? null : messages.get(messages.size() - 1).messageId();

        return ChatHistoryResponse.of(messages, nextCursorId, hasNext);
    }

    private boolean addMongoMessages(Long roomId, String cursorId, int remainingNeeded, List<MessageResponse> messages) {
        String nextCursorId = messages.isEmpty() ? cursorId : messages.get(messages.size() - 1).messageId();

        Pageable pageable = PageRequest.of(0, remainingNeeded + 1, Sort.by(Sort.Direction.DESC, "_id"));

        List<Message> mongoMessages;
        if (nextCursorId == null) {
            mongoMessages = messageRepository.findByRoomId(roomId, pageable);
        } else {
            mongoMessages = messageRepository.findByRoomIdAndIdLessThan(roomId, nextCursorId, pageable);
        }

        boolean hasMore = mongoMessages.size() > remainingNeeded;

        List<MessageResponse> toAdd = mongoMessages.stream()
                .limit(remainingNeeded)
                .map(MessageResponse::from)
                .toList();

        messages.addAll(toAdd);

        return hasMore;
    }
}
