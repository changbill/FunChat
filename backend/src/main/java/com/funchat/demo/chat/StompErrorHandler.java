package com.funchat.demo.chat;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    public StompErrorHandler() {
        super();
    }

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        // 원인 예외 찾기 (BusinessException 추출)
        Throwable cause = ex.getCause();

        if (cause instanceof BusinessException businessException) {
            return prepareErrorMessage(businessException.errorCode());
        }

        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> prepareErrorMessage(ErrorCode errorCode) {
        String code = errorCode.name();
        String message = errorCode.getMessage();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(message); // ERROR 프레임의 message 헤더
        accessor.setLeaveMutable(true);

        // 추가적인 에러 코드를 헤더에 삽입 가능
        accessor.setNativeHeader("code", code);

        return MessageBuilder.createMessage(
                message.getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}