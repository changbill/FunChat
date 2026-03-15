package com.funchat.demo.room.service;

import com.funchat.demo.chat.domain.ChatRoomRepository;
import com.funchat.demo.chat.domain.dto.ChatRoomResponse;
import com.funchat.demo.chat.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final ChatRoomRepository chatRoomRepository;

    public List<ChatRoomResponse> getChatRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllWithUsers();

        return chatRooms.stream().map(
                chatRoom -> ChatRoomResponse.builder()
                        .id(chatRoom.getId())
                        .name(chatRoom.getName())
                        .participantCount((long) chatRoom.getChatRoomUsers().size())
                        .build()
        ).collect(Collectors.toList());
    }

    // todo: spring security 적용 이후 @AuthenticationPrincipal을 통해 room에 user 넣기
    public ChatRoomResponse createChatRoom(RoomRequest roomRequest) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomRequest.name())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        return new ChatRoomResponse(savedRoom.getId(), savedRoom.getName(), 0L);
    }
}
