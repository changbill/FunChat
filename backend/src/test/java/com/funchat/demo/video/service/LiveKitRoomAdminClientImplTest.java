package com.funchat.demo.video.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class LiveKitRoomAdminClientImplTest {

    private HttpServer server;

    @Test
    void deleteMissingRoomIsAlreadyComplete() throws IOException {
        LiveKitRoomAdminClientImpl client = createClient(404);
        assertThatCode(() -> client.deleteRoom("funchat-room-1")).doesNotThrowAnyException();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("LiveKit room 삭제 2xx 응답을 성공으로 처리한다")
    void deleteRoom_WhenLiveKitReturnsSuccess_Completes() throws IOException {
        LiveKitRoomAdminClientImpl client = createClient(200);

        assertThatCode(() -> client.deleteRoom("funchat-room-1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("LiveKit room 삭제 5xx 응답을 영상 방 정리 실패로 변환한다")
    void deleteRoom_WhenLiveKitReturnsServerError_ThrowsBusinessException() throws IOException {
        LiveKitRoomAdminClientImpl client = createClient(500);

        assertThatThrownBy(() -> client.deleteRoom("funchat-room-1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_ROOM_CLEANUP_FAILED);
    }

    private LiveKitRoomAdminClientImpl createClient(int statusCode) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/protobuf");
            exchange.sendResponseHeaders(statusCode, 0);
            exchange.close();
        });
        server.start();

        LiveKitProperties properties = new LiveKitProperties(
                "test-key",
                "test-secret-value-for-hmac-signing",
                "ws://localhost:" + server.getAddress().getPort(),
                3600
        );
        return new LiveKitRoomAdminClientImpl(properties);
    }
}
