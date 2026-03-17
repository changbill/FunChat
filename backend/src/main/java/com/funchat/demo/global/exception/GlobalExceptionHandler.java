package com.funchat.demo.global.exception;

import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseDto> handleBusinessException(BusinessException e) {
        ErrorCode code = e.errorCode();
        log.warn("비즈니스 예외가 발생했습니다. code={}, message={}", code, e.getMessage());
        return ResponseUtil.createErrorResponse(code, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleValidationException(MethodArgumentNotValidException e) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        log.warn("요청 검증 예외가 발생했습니다. message={}", e.getMessage());
        return ResponseUtil.createErrorResponse(code, e.getBody());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ErrorCode code = ErrorCode.INVALID_REQUEST; // JSON 파싱 실패 등
        log.warn("메시지 읽기 실패: message={}", e.getMessage());
        return ResponseUtil.createErrorResponse(code, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto> handleException(Exception e) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR; // "예상치 못한 오류가 발생했습니다."
        log.error("서버 내부 오류 발생: message={}", e.getMessage(), e);
        return ResponseUtil.createErrorResponse(code, null);
    }
}