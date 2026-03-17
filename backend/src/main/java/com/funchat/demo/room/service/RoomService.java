package com.funchat.demo.room.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.room.domain.RoomUser;
import com.funchat.demo.room.domain.RoomUserRepository;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Room room = Room.builder()
                .title(request.title())
                .maxMembers(request.maxMembers())
                .manager(manager)
                .createdAt(LocalDateTime.now())
                .build();

        roomRepository.save(room);

        // 방장 자동 참여
        RoomUser roomUser = RoomUser.builder()
                .room(room)
                .user(manager)
                .joinedAt(LocalDateTime.now())
                .build();
        roomUserRepository.save(roomUser);

        return RoomResponse.from(room);
    }

    public List<RoomResponse> findAllRooms() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    public RoomResponse findRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpdateRequest request) {
        // 1. 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 2. 권한 확인 (방장만 수정 가능)
        if (!room.getManager().getId().equals(request.userId())) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        // 3. 인원수 수정 시 현재 인원보다 적게 수정하는지 검증
        if (request.maxMembers() < room.getRoomUsers().size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 또는 적절한 에러코드
        }

        // 4. 더티 체킹에 의한 수정
        room.update(request.title(), request.maxMembers());

        return RoomResponse.from(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 권한 체크: 방장만 삭제 가능
        if (!room.getManager().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        roomRepository.delete(room);
    }
}
