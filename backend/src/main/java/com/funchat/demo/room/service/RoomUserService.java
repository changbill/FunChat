package com.funchat.demo.room.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.room.domain.RoomUser;
import com.funchat.demo.room.domain.RoomUserRepository;
import com.funchat.demo.room.domain.dto.ParticipantResponse;
import com.funchat.demo.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomUserService {

    private final RoomUserRepository roomUserRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        // 1. 방/유저 존재 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 중복 참여 확인
        if (roomUserRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_ALREADY_JOINED);
        }

        // 3. 인원 제한 확인
        if (roomUserRepository.countByRoomId(roomId) >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.ROOM_MAX_CAPACITY_REACHED);
        }

        // 4. 입장 처리
        RoomUser roomUser = RoomUser.builder()
                .room(room)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
        roomUserRepository.save(roomUser);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        RoomUser roomUser = roomUserRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT));

        roomUserRepository.delete(roomUser);
    }

    public List<ParticipantResponse> getParticipants(Long roomId) {
        // 방 존재 여부 우선 확인
        if (!roomRepository.existsById(roomId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        return roomUserRepository.findAllByRoomIdWithUser(roomId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }
}