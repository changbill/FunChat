package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.ChatMessage;
import com.funchat.demo.chat.domain.MessageRepository;
import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.domain.dto.ChatHistoryResponse;
import com.funchat.demo.chat.domain.dto.MessageResponse;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.util.ParseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.funchat.demo.global.constants.ChatConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;

    public void saveMessageToMongo(Map<String, String> messageMap) {
        String typeRaw = messageMap.get(MESSAGE_TYPE);
        if (typeRaw == null || typeRaw.isBlank()) {
            typeRaw = MessageType.TEXT.name();
        }

        ChatMessage message = ChatMessage.createMessage(
                ParseUtil.parseLong(messageMap.get(ROOM_ID), new BusinessException(ErrorCode.ROOM_NOT_FOUND, "roomId 형식이 잘못되었습니다.")),
                ParseUtil.parseLong(messageMap.get(SENDER_ID), new BusinessException(ErrorCode.USER_NOT_FOUND, "senderId 형식이 잘못되었습니다.")),
                messageMap.get(SENDER_NICKNAME),
                messageMap.get(MESSAGE_CONTENT),
                MessageType.valueOf(typeRaw)
        );

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
