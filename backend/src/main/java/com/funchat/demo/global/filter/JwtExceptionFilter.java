package com.funchat.demo.global.filter;

import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * filter는 스프링MVC의 DispatcherServlet보다 앞단 서블릿 컨테이너 레벨에 위치.
 * ExceptionHandler는 컨트롤러 어드바이스로,
 * DispatcherServlet 내부의 컨트롤러 계층에서 발생하는 예외만 처리 가능.
 *
 * 필터에서 발생하는 예외를 잡으려면 예외가 발생할 필터보다 더 앞에 있는 "예외 처리 전용 필터"를 하나 더 만들어 처리해야함.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            log.warn("JWT 필터에서 예외가 발생했습니다. message={}", e.errorCode().getMessage());
            setErrorResponse(response, e.errorCode());
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ResponseDto responseDto = new ResponseDto(
                errorCode.getStatus().value(),
                errorCode.getMessage(),
                null
        );

        String jsonResponse = objectMapper.writeValueAsString(responseDto);
        response.getWriter().write(jsonResponse);
    }
}