package com.funchat.demo.video.service;

import com.funchat.demo.chat.service.ChatFanoutBroker;
import com.funchat.demo.chat.service.ChatPersistBroker;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import com.funchat.demo.video.domain.VideoSession;
import com.funchat.demo.video.domain.VideoSessionRepository;
import com.funchat.demo.video.domain.VideoSessionStatus;
import com.funchat.demo.video.domain.dto.VideoSessionResponse;
import com.funchat.demo.video.domain.dto.VideoTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VideoServiceTest {

    private static final String TEST_LIVEKIT_SECRET = "test-livekit-secret-value-for-hmac-signing";

    @Autowired
    private VideoService videoService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoSessionRepository videoSessionRepository;

    @MockitoBean
    private ChatPersistBroker chatPersistBroker;

    @MockitoBean
    private ChatFanoutBroker chatFanoutBroker;

    @MockitoBean
    private LiveKitRoomAdminClient liveKitRoomAdminClient;

    @Test
    @DisplayName("방 참여자는 영상 세션을 시작할 수 있고 기존 활성 세션을 재사용한다")
    void startSession_ReusesActiveSession() {
        TestRoom testRoom = saveRoom("영상방", 5);

        VideoSessionResponse first = videoService.startSession(testRoom.room().getId(), testRoom.manager().getId());
        VideoSessionResponse second = videoService.startSession(testRoom.room().getId(), testRoom.manager().getId());

        assertThat(first.sessionId()).isEqualTo(second.sessionId());
        assertThat(first.livekitRoomName()).isEqualTo("funchat-room-" + testRoom.room().getId());
        assertThat(videoSessionRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("방 참여자가 아니면 영상 세션을 시작할 수 없다")
    void startSession_WhenUserIsNotParticipant_ThrowsException() {
        TestRoom testRoom = saveRoom("영상방", 5);
        User outsider = saveUser("outsider@test.com", "outsider");

        assertThatThrownBy(() -> videoService.startSession(testRoom.room().getId(), outsider.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
    }

    @Test
    @DisplayName("활성 세션이 없으면 조회에 실패한다")
    void findActiveSession_WhenMissing_ThrowsException() {
        TestRoom testRoom = saveRoom("영상방", 5);

        assertThatThrownBy(() -> videoService.findActiveSession(testRoom.room().getId(), testRoom.manager().getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("참가 토큰은 활성 세션을 만들고 LiveKit join/publish/subscribe grant를 포함한다")
    void issueJoinToken_ReturnsLiveKitToken() {
        TestRoom testRoom = saveRoom("영상방", 5);

        VideoTokenResponse response = videoService.issueJoinToken(testRoom.room().getId(), testRoom.manager().getId());
        Claims claims = parseClaims(response.token());
        @SuppressWarnings("unchecked")
        Map<String, Object> videoGrant = claims.get("video", Map.class);

        assertThat(response.livekitUrl()).isEqualTo("http://localhost:7880");
        assertThat(response.livekitRoomName()).isEqualTo("funchat-room-" + testRoom.room().getId());
        assertThat(response.identity()).isEqualTo("user-" + testRoom.manager().getId());
        assertThat(claims.getIssuer()).isEqualTo("test-livekit-key");
        assertThat(claims.getSubject()).isEqualTo(response.identity());
        assertThat(claims.get("name", String.class)).isEqualTo(testRoom.manager().getNickname());
        assertThat(videoGrant)
                .containsEntry("room", response.livekitRoomName())
                .containsEntry("roomJoin", true)
                .containsEntry("canPublish", true)
                .containsEntry("canPublishData", true)
                .containsEntry("canSubscribe", true);
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("방 매니저는 활성 영상 세션을 종료할 수 있다")
    void endSession_WhenManager_EndsActiveSession() {
        TestRoom testRoom = saveRoom("영상방", 5);
        VideoSessionResponse started = videoService.startSession(testRoom.room().getId(), testRoom.manager().getId());

        VideoSessionResponse ended = videoService.endSession(
                testRoom.room().getId(),
                started.sessionId(),
                testRoom.manager().getId()
        );

        VideoSession session = videoSessionRepository.findById(started.sessionId()).orElseThrow();
        assertThat(ended.status()).isEqualTo(VideoSessionStatus.ENDED);
        assertThat(ended.endedAt()).isNotNull();
        assertThat(session.getStatus()).isEqualTo(VideoSessionStatus.ENDED);
        assertThat(session.getEndedAt()).isNotNull();
        verify(liveKitRoomAdminClient).deleteRoom(started.livekitRoomName());
    }

    @Test
    @DisplayName("LiveKit 방 정리에 실패하면 영상 세션을 활성 상태로 유지한다")
    void endSession_WhenLiveKitCleanupFails_KeepsSessionActive() {
        TestRoom testRoom = saveRoom("영상방", 5);
        VideoSessionResponse started = videoService.startSession(testRoom.room().getId(), testRoom.manager().getId());
        doThrow(new BusinessException(ErrorCode.VIDEO_ROOM_CLEANUP_FAILED))
                .when(liveKitRoomAdminClient)
                .deleteRoom(started.livekitRoomName());

        assertThatThrownBy(() -> videoService.endSession(
                testRoom.room().getId(),
                started.sessionId(),
                testRoom.manager().getId()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_ROOM_CLEANUP_FAILED);

        VideoSession session = videoSessionRepository.findById(started.sessionId()).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(VideoSessionStatus.ACTIVE);
        assertThat(session.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("방 매니저가 아니면 영상 세션을 종료할 수 없다")
    void endSession_WhenUserIsNotManager_ThrowsException() {
        TestRoom testRoom = saveRoom("영상방", 5);
        User participant = saveUser("participant@test.com", "participant");
        testRoom.room().acceptParticipant(participant, testRoom.room().getParticipants().size());
        VideoSessionResponse started = videoService.startSession(testRoom.room().getId(), testRoom.manager().getId());

        assertThatThrownBy(() -> videoService.endSession(testRoom.room().getId(), started.sessionId(), participant.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
    }

    @Test
    @DisplayName("활성 세션이 없으면 영상 세션 종료에 실패한다")
    void endSession_WhenActiveSessionIsMissing_ThrowsException() {
        TestRoom testRoom = saveRoom("영상방", 5);

        assertThatThrownBy(() -> videoService.endSession(testRoom.room().getId(), 999L, testRoom.manager().getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_SESSION_NOT_FOUND);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_LIVEKIT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    private User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.createUser(email, "password", nickname, ""));
    }

    private TestRoom saveRoom(String title, int maxMembers) {
        User manager = saveUser("manager-" + System.nanoTime() + "@test.com", "manager" + System.nanoTime());
        Room room = roomRepository.saveAndFlush(Room.createRoom(title, maxMembers, manager));
        return new TestRoom(room, manager);
    }

    private record TestRoom(Room room, User manager) {
    }
}
