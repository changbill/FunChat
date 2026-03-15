package com.funchat.demo.global.exception;

import com.funchat.demo.global.dto.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import static com.funchat.demo.util.ResponseUtil.createErrorResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String ERROR_MESSAGE = "에러 발생 : {}";
    private static final String FIELD = "field";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);

        Map<String, String> fieldInfo = new HashMap<>();
        fieldInfo.put(FIELD, fieldError.getField());

        log.warn(ERROR_MESSAGE, ex.getMessage(), ex);
        return createErrorResponse(
                ErrorCode.VALIDATION_FAILED,
                fieldError.getDefaultMessage(),
                fieldInfo
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn(ERROR_MESSAGE, ex.getMessage(), ex);
        return createErrorResponse(ErrorCode.BAD_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto> handleException(Exception ex) {
        log.error(ERROR_MESSAGE, ex.getMessage(), ex);
        return createErrorResponse(ErrorCode.SERVER_ERROR, null);
    }
}