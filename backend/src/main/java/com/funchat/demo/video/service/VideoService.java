package com.funchat.demo.video.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private static final String LIVEKIT_ROOM_PREFIX = "funchat-room-";
    private static final String LIVEKIT_IDENTITY_PREFIX = "user-";

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final VideoSessionRepository videoSessionRepository;
    private final LiveKitTokenProvider liveKitTokenProvider;
    private final LiveKitRoomAdminClient liveKitRoomAdminClient;

    @Transactional
    public VideoSessionResponse startSession(Long roomId, Long userId) {
        Room room = findRoom(roomId);
        validateParticipant(roomId, userId);

        VideoSession session = findActiveSession(roomId)
                .orElseGet(() -> videoSessionRepository.save(
                        VideoSession.start(room, createLiveKitRoomName(roomId))
                ));

        return VideoSessionResponse.from(session);
    }

    public VideoSessionResponse findActiveSession(Long roomId, Long userId) {
        findRoom(roomId);
        validateParticipant(roomId, userId);

        VideoSession session = findActiveSession(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_SESSION_NOT_FOUND));

        return VideoSessionResponse.from(session);
    }

    @Transactional
    public VideoTokenResponse issueJoinToken(Long roomId, Long userId) {
        Room room = findRoom(roomId);
        User user = validateParticipant(roomId, userId);
        VideoSession session = findActiveSession(roomId)
                .orElseGet(() -> videoSessionRepository.save(
                        VideoSession.start(room, createLiveKitRoomName(roomId))
                ));

        String identity = LIVEKIT_IDENTITY_PREFIX + user.getId();
        LiveKitTokenProvider.LiveKitIssuedToken issuedToken = liveKitTokenProvider.issueJoinToken(
                session.getLivekitRoomName(),
                identity,
                user.getNickname()
        );

        return VideoTokenResponse.builder()
                .sessionId(session.getId())
                .roomId(room.getId())
                .livekitUrl(liveKitTokenProvider.liveKitUrl())
                .livekitRoomName(session.getLivekitRoomName())
                .identity(identity)
                .participantName(user.getNickname())
                .token(issuedToken.token())
                .expiresAt(LocalDateTime.ofInstant(issuedToken.expiresAt(), ZoneId.systemDefault()))
                .build();
    }

    @Transactional
    public VideoSessionResponse endSession(Long roomId, Long sessionId, Long userId) {
        Room room = findRoom(roomId);
        validateManager(room, userId);

        VideoSession session = findActiveSession(roomId)
                .filter(activeSession -> activeSession.getId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_SESSION_NOT_FOUND));

        liveKitRoomAdminClient.deleteRoom(session.getLivekitRoomName());
        session.end();

        return VideoSessionResponse.from(session);
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private User validateParticipant(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!userRepository.existsByIdAndRoom_Id(userId, roomId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }

        return user;
    }

    private void validateManager(Room room, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!room.getManager().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }
    }

    private java.util.Optional<VideoSession> findActiveSession(Long roomId) {
        return videoSessionRepository.findFirstByRoomIdAndStatusOrderByIdDesc(roomId, VideoSessionStatus.ACTIVE);
    }

    private String createLiveKitRoomName(Long roomId) {
        return LIVEKIT_ROOM_PREFIX + roomId;
    }
}
