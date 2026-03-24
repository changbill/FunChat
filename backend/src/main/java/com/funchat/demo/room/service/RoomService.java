package com.funchat.demo.room.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.chat.service.redis.RedisService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final MessageBrokerChatService messageBrokerChatService;

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        User manager = findUserById(request.managerId());

        Room room = Room.createRoom(request.title(), request.maxMembers(), manager);
        roomRepository.save(room);

        return RoomResponse.from(room);
    }

    // TODO: participants 전부 불러오는 문제 해결
    public List<RoomResponse> findAllRooms() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    // TODO: participants 전부 불러오는 문제 해결
    public RoomResponse findRoom(Long roomId) {
        return RoomResponse.from(findRoomById(roomId));
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpdateRequest request, Long userId) {
        Room room = findRoomByIdWithParticipants(roomId);

        if (!room.getManager().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }

        if (request.maxMembers() < room.getParticipants().size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 또는 적절한 에러코드
        }

        room.updateRoom(request.title(), request.maxMembers());

        return RoomResponse.from(room);
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
        Room room = findRoomByIdWithParticipants(roomId);
        User user = findUserById(userId);
        if(room.getBannedUserIds().contains(userId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_BANNED);
        }

        if(room.getParticipants().size() >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.ROOM_MAX_CAPACITY_REACHED);
        }

        room.acceptParticipant(user);
        redisService.saveUserCurrentRoomId(userId, roomId);

        return RoomResponse.from(room);
    }

    @Transactional
    public void leaveRoom(Long userId) {
        User user = findUserById(userId);
        Room room = roomRepository.findByIdWithParticipants(user.getRoom().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        boolean isRoomManager = user.isRoomManager();
        user.leaveRoom();

        if (isRoomManager) {
            Optional<User> newManagerOpt = room.getParticipants().stream()
                    .filter(p -> !p.getId().equals(userId))
                    .findFirst();

            if (newManagerOpt.isEmpty()) {
                roomRepository.delete(room);
                return;
            }

            User newManager = newManagerOpt.get();
            room.delegateManager(newManager);
            messageBrokerChatService.sendNoticeToRedisStreams(room.getId(), newManager.getNickname(), MessageType.DELEGATE);
        }

        redisService.deleteUserCurrentRoomId(userId);
    }

    @Transactional
    public void banUser(Long roomId, Long managerId, Long userId) {
        Room room = findRoomByIdWithParticipants(roomId);
        if( !room.getManager().getId().equals(managerId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_MANAGER);
        }
        User user = findUserById(userId);

        if(!room.isParticipant(user)) {
            throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }
        room.setBannedUsers(user);
        user.leaveRoom();

        messageBrokerChatService.sendNoticeToRedisStreams(room.getId(), user.getNickname(), MessageType.BAN);
        redisService.deleteUserCurrentRoomId(userId);
    }

    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Room findRoomByIdWithParticipants(Long roomId) {
        return roomRepository.findByIdWithParticipants(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
