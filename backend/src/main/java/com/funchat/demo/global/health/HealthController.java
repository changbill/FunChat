package com.funchat.demo.global.health;

import com.funchat.demo.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping("/health")
    public ResponseEntity<ResponseDto> health() {
        HealthCheckResponse response = healthService.check();
        HttpStatus status = response.healthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        String message = response.healthy() ? "성공" : "서비스 준비 상태가 아닙니다.";

        return ResponseEntity.status(status)
                .body(new ResponseDto(status.value(), message, response));
    }
}
