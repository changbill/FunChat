package com.funchat.demo.video.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import io.livekit.server.RoomServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LiveKitRoomAdminClientImpl implements LiveKitRoomAdminClient {

    private final LiveKitProperties properties;

    @Override
    public void deleteRoom(String roomName) {
        try {
            Response<?> response = createClient().deleteRoom(roomName).execute();
            if (!response.isSuccessful()) {
                throw new BusinessException(
                        ErrorCode.VIDEO_ROOM_CLEANUP_FAILED,
                        "LiveKit room cleanup failed with status " + response.code()
                );
            }
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.VIDEO_ROOM_CLEANUP_FAILED,
                    ErrorCode.VIDEO_ROOM_CLEANUP_FAILED.getMessage(),
                    exception
            );
        }
    }

    private RoomServiceClient createClient() {
        return RoomServiceClient.createClient(
                toHttpUrl(properties.url()),
                properties.apiKey(),
                properties.apiSecret()
        );
    }

    private String toHttpUrl(String url) {
        if (url.startsWith("wss://")) {
            return "https://" + url.substring("wss://".length());
        }
        if (url.startsWith("ws://")) {
            return "http://" + url.substring("ws://".length());
        }
        return url;
    }
}
