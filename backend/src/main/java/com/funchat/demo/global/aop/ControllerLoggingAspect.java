package com.funchat.demo.global.aop;

import com.funchat.demo.global.annotation.LogMask;
import com.funchat.demo.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @RestController 메서드 실행 시 요청/응답 로그를 기록한다.
 */
@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String logPrefix = className + "." + methodName;

        HttpServletRequest request = getCurrentRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";

        String params = buildParameterLog(signature.getParameterNames(), joinPoint.getArgs(), signature.getMethod().getParameters());
        log.info("{} - [{} {}] Request started {}", logPrefix, httpMethod, requestUri, params);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (result instanceof ResponseEntity<?> response) {
                log.info(
                        "{} - [{} {}] Request completed successfully ({}ms) - status={}",
                        logPrefix,
                        httpMethod,
                        requestUri,
                        executionTime,
                        response.getStatusCode()
                );
            } else {
                log.info("{} - [{} {}] Request completed successfully ({}ms)", logPrefix, httpMethod, requestUri, executionTime);
            }
            return result;
        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error(
                    "{} - [{} {}] Request failed ({}ms): {}",
                    logPrefix,
                    httpMethod,
                    requestUri,
                    executionTime,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String buildParameterLog(String[] paramNames, Object[] args, Parameter[] parameters) {
        if (paramNames == null || paramNames.length == 0 || args == null || args.length == 0) {
            return "";
        }

        List<String> logParams = new ArrayList<>();
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            Object value = args[i];
            String name = paramNames[i];
            Parameter parameter = i < parameters.length ? parameters[i] : null;
            appendParameterLog(value, parameter, logParams, name);
        }

        return logParams.isEmpty() ? "" : "with [" + String.join(", ", logParams) + "]";
    }

    private void appendParameterLog(Object value, Parameter parameter, List<String> logParams, String name) {
        if (value == null) {
            return;
        }

        if (parameter != null && parameter.isAnnotationPresent(LogMask.class)) {
            logParams.add(name + "=[PROTECTED]");
            return;
        }
        logParams.add(formatValue(name, value));
    }

    private String formatValue(String name, Object value) {
        return switch (value) {
            case User user -> name + "=" + user.getId();
            case String stringValue -> name + "='" + stringValue + "'";
            case Enum<?> enumValue -> name + "=" + enumValue;
            case Number numberValue -> name + "=" + numberValue;
            case Boolean boolValue -> name + "=" + boolValue;
            default -> name + "=" + value.getClass().getSimpleName();
        };
    }
}
