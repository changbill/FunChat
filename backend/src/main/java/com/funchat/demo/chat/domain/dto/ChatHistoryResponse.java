package com.funchat.demo.chat.domain.dto;

import java.util.List;


public record ChatHistoryResponse(
        List<MessageResponse> messages,
        String nextCursorId,
        boolean hasNext
) {
    public static ChatHistoryResponse of(List<MessageResponse> messages,
                                         String nextCursorId,
                                         boolean hasNext) {
        return new ChatHistoryResponse(messages, nextCursorId, hasNext);
    }
}
