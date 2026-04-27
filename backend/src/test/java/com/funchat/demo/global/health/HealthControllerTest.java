package com.funchat.demo.global.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("모든 의존성이 정상일 때 /health는 200을 반환한다")
    void health_Up() throws Exception {
        when(healthService.check()).thenReturn(new HealthCheckResponse(
                "UP",
                Map.of("database", "UP", "redis", "UP", "mongodb", "UP")
        ));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.body.status").value("UP"))
                .andExpect(jsonPath("$.body.checks.database").value("UP"));
    }

    @Test
    @DisplayName("의존성 중 하나라도 실패하면 /health는 503을 반환한다")
    void health_Down() throws Exception {
        when(healthService.check()).thenReturn(new HealthCheckResponse(
                "DOWN",
                Map.of("database", "UP", "redis", "DOWN", "mongodb", "UP")
        ));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("서비스 준비 상태가 아닙니다."))
                .andExpect(jsonPath("$.body.status").value("DOWN"))
                .andExpect(jsonPath("$.body.checks.redis").value("DOWN"));
    }
}
