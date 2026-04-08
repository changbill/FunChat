package com.funchat.demo.room.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MessageBrokerChatService messageBrokerChatService;

    @Transactional
    public RoomResponse createRoom(RoomRequest request, Long userId) {
        User manager = findUserById(userId);

        Room room = Room.createRoom(request.title(), request.maxMembers(), manager);
        roomRepository.save(room);

        long roomId = room.getId();
        long currentCount = userRepository.countByRoomId(roomId);
        return RoomResponse.from(room, currentCount);
    }

    public Page<RoomResponse> findAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable)
                .map(room -> {
                    long currentCount = userRepository.countByRoomId(room.getId());
                    return RoomResponse.from(room, currentCount);
                });
    }

    public RoomResponse findRoom(Long roomId) {
        long currentCount = userRepository.countByRoomId(roomId);
        return RoomResponse.from(findRoomById(roomId), currentCount);
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpdateRequest request, Long userId) {
        Room room = findRoomById(roomId);

        if (!room.getManager().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        long currentCount = userRepository.countByRoomId(roomId);
        if (request.maxMembers() < currentCount) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 또는 적절한 에러코드
        }

        room.updateRoom(request.title(), request.maxMembers());

        return RoomResponse.from(room, currentCount);
    }

    @Transactional
    public void delegateManager(Long roomId, Long managerId, Long newManagerId) {
        if (!userRepository.existsById(managerId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Room room = findRoomById(roomId);

        if (!room.getManager().getId().equals(managerId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        User newManager = findUserById(newManagerId);

        room.delegateManager(newManager);
        messageBrokerChatService.sendNoticeToRedisStreams(room.getId(), newManager.getNickname(), MessageType.DELEGATE);
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        Room room = findRoomById(roomId);

        if (!room.getManager().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        room.deleteSetting();
        roomRepository.delete(room);
    }

    @Transactional
    public RoomResponse enterRoom(Long roomId, Long userId) {
        Room room = findRoomById(roomId);
        User user = findUserById(userId);
        if (room.getBannedUserIds().contains(userId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_BANNED);
        }

        long currentCount = userRepository.countByRoomId(roomId);
        room.acceptParticipant(user, currentCount);

        messageBrokerChatService.sendNoticeToRedisStreams(room.getId(), user.getNickname(), MessageType.JOIN);

        currentCount = userRepository.countByRoomId(roomId);
        return RoomResponse.from(room, currentCount);
    }

    @Transactional
    public void leaveRoom(Long userId) {
        User user = findUserById(userId);
        if (user.getRoom() == null) return;
        Room room = findRoomById(user.getRoom().getId());

        Long roomId = room.getId();
        String userNickname = user.getNickname();
        boolean wasManager = room.getManager().getId().equals(userId);

        if (wasManager) {
            Optional<User> newManagerOpt = userRepository.findFirstByRoomIdAndIdNot(roomId, userId);

            if (newManagerOpt.isPresent()) {
                User newManager = newManagerOpt.get();
                room.delegateManager(newManager);
                messageBrokerChatService.sendNoticeToRedisStreams(roomId, newManager.getNickname(), MessageType.DELEGATE);
            } else {
                roomRepository.delete(room);
            }
        }

        user.leaveRoom();

        messageBrokerChatService.sendNoticeToRedisStreams(roomId, userNickname, MessageType.LEAVE);
    }

    @Transactional
    public void banUser(Long roomId, Long managerId, Long userId) {
        Room room = findRoomById(roomId);
        if (!room.getManager().getId().equals(managerId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        User user = findUserById(userId);

        if (!userRepository.existsByIdAndRoom_Id(userId, roomId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }

        room.setBannedUsers(user);
        user.leaveRoom();

        messageBrokerChatService.sendNoticeToRedisStreams(room.getId(), user.getNickname(), MessageType.BAN);
    }

    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
