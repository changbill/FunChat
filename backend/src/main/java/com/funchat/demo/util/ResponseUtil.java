package com.funchat.demo.util;

import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {
    private static final String DEFAULT_SUCCESS_MESSAGE = "성공";
    private static final HttpStatus DEFAULT_SUCCESS_STATUS = HttpStatus.OK;

    public static ResponseEntity<ResponseDto> createSuccessResponse(Object body) {
        return createSuccessResponse(DEFAULT_SUCCESS_STATUS, DEFAULT_SUCCESS_MESSAGE, body);
    }

    public static ResponseEntity<ResponseDto> createSuccessResponse(String message, Object body) {
        return createSuccessResponse(DEFAULT_SUCCESS_STATUS, message, body);
    }

    public static ResponseEntity<ResponseDto> createSuccessResponse(HttpStatus status, String message, Object body) {
        return ResponseEntity.status(status)
                .body(new ResponseDto(status.value(),message,body));
    }

    public static ResponseEntity<ResponseDto> createErrorResponse(ErrorCode errorCode, Object body) {
        return createErrorResponse(errorCode, errorCode.getMessage(), body);
    }

    public static ResponseEntity<ResponseDto> createErrorResponse(ErrorCode errorCode, String message, Object body) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ResponseDto(
                        errorCode.getStatus().value(),
                        message,
                        body
                ));
    }
}
