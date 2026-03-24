package com.funchat.demo.room.service;

import com.funchat.demo.chat.domain.MessageType;
import com.funchat.demo.chat.service.MessageBrokerChatService;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private MessageBrokerChatService messageBrokerChatService;

    @InjectMocks
    private RoomService roomService;

    private User manager;
    private User participant;
    private User newbie;
    private Room room;

    @BeforeEach
    void setUp() {
        manager = User.createForTest(1L, "abc@abc.com", "매니저", "asdasd.png");
        participant = User.createForTest(2L, "qwe@qwe.com", "참여자", "qweqwe.png");
        newbie = User.createForTest(3L, "newbie@asd.com", "뉴비", "newbie.png");
        room = Room.createForTest(1L, "테스트 방", 5, manager);
    }

    @Nested
    @DisplayName("방 정보 수정")
    class UpdateRoom {
        @Test
        @DisplayName("바꾸려는 사람이 매니저가 아닌 경우")
        void notManager() {
            // given
            room.acceptParticipant(participant);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when & then
            RoomUpdateRequest request = RoomUpdateRequest.builder().build();
            assertThatThrownBy(() -> roomService.updateRoom(1L, request, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("최대 인원 설정은 현재 인원보다 더 적게 설정할 수 없다")
        void cantSetMaxMembers_LessThen_CurrentNumberOfPeople() {
            // given
            room.acceptParticipant(participant);
            room.acceptParticipant(newbie);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when & then
            RoomUpdateRequest request = RoomUpdateRequest.builder().maxMembers(2).build();
            assertThatThrownBy(() -> roomService.updateRoom(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("성공")
        void Success() {
            // given
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when
            RoomUpdateRequest request = new RoomUpdateRequest("수정된 방 제목", 10);
            RoomResponse response = roomService.updateRoom(1L, request, 1L);

            // then
            assertThat(response.title()).isEqualTo("수정된 방 제목");
            assertThat(response.maxMembers()).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("방 만들기")
    void createRoom() {
        // given
        RoomRequest request = new RoomRequest("새로운 방", 10, 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

        // when & then
        RoomResponse response = roomService.createRoom(request);

        assertThat(response.title()).isEqualTo("새로운 방");
        assertThat(response.maxMembers()).isEqualTo(10);
        assertThat(participant.getRoom()).isNotNull();
        assertThat(participant.getRoom().getTitle()).isEqualTo("새로운 방");
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Nested
    @DisplayName("방장 위임")
    class DelegateManager {
        @Test
        @DisplayName("방장 아이디 찾을 수 없는 경우")
        void notFoundManager() {
            //given
            room.acceptParticipant(participant);
            when(userRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> roomService.delegateManager(1L, 1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("방장 권한이 없는 경우")
        void notManager() {
            // given
            room.acceptParticipant(participant);
            room.acceptParticipant(newbie);
            when(userRepository.existsById(2L)).thenReturn(true);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> roomService.delegateManager(1L, 2L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("성공")
        void Success() {
            room.acceptParticipant(participant);
            when(userRepository.existsById(1L)).thenReturn(true);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

            roomService.delegateManager(1L, 1L, 2L);

            assertThat(room.getManager()).isEqualTo(participant);
        }
    }

    @Nested
    @DisplayName("방 삭제")
    class DeleteRoom {
        @Test
        @DisplayName("방장 권한이 없는 경우")
        void notManager() {
            // given
            room.acceptParticipant(participant);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.deleteRoom(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("참여자와 방 연관관계가 끊어졌는지 확인")
        void disconnectRelation() {
            // given
            room.acceptParticipant(participant);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            // when
            roomService.deleteRoom(1L, 1L);

            assertThat(participant.getRoom()).isNull();
        }

        @Test
        @DisplayName("성공")
        void success() {
            // given
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            // when
            roomService.deleteRoom(1L, 1L);

            verify(roomRepository, times(1)).delete(room);
        }
    }

    @Nested
    @DisplayName("방 입장")
    class EnterRoom {
        @Test
        @DisplayName("방 id가 잘못된 경우")
        void invalidRoomId() {
            // given
            when(roomRepository.findByIdWithParticipants(2L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("유저를 찾을 수 없는 경우")
        void invalidUserId() {
            // given
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(4L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(1L, 4L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("강퇴당한 유저 입장 불가")
        void cantEnter_BannedUser() {
            // given
            room.setBannedUsers(participant);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_BANNED);
        }

        @Test
        @DisplayName("최대 인원 이상 입장 불가")
        void cantEnter_MaxMembers() {
            // given
            room.updateRoom("방 이름", 2);
            room.acceptParticipant(participant);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(3L)).thenReturn(Optional.of(newbie));

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(1L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_MAX_CAPACITY_REACHED);
        }

        @Test
        @DisplayName("성공")
        void success() {
            //given
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

            // when
            roomService.enterRoom(1L, 2L);

            // then
            assertThat(participant.getRoom()).isEqualTo(room);
            assertThat(room.getParticipants()).contains(participant);
            assertThat(room.getParticipants()).hasSize(2);
            verify(redisService, times(1)).saveUserCurrentRoomId(2L, 1L);
        }
    }

    @Nested
    @DisplayName("방 나가기")
    class LeaveRoom {
        @Test
        @DisplayName("일반 참여자가 나가면 방은 유지되고 참여자 명단에서만 제거된다")
        void leaveRoom_Participant() {
            // given
            room.acceptParticipant(participant);

            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when
            roomService.leaveRoom(2L);

            // then
            assertThat(room.getParticipants()).hasSize(1);
            assertThat(room.getParticipants()).contains(manager);
            assertThat(participant.getRoom()).isNull();
        }

        @Test
        @DisplayName("방장이 나가면 다음 참여자에게 방장이 위임된다")
        void leaveRoom_DelegateManager() {
            // given
            room.acceptParticipant(participant);

            when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when
            roomService.leaveRoom(1L);

            // then
            assertThat(room.getManager()).isEqualTo(participant); // 방장이 참여자로 변경됨
            assertThat(room.getParticipants()).hasSize(1);
            assertThat(room.getParticipants()).doesNotContain(manager);
        }

        @Test
        @DisplayName("마지막 남은 사람이 나가면 방이 삭제된다")
        void leaveRoom_DeleteRoomWhenLastPersonLeaves() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when
            roomService.leaveRoom(1L);

            // then
            verify(roomRepository, times(1)).delete(room);
        }
    }

    @Nested
    @DisplayName("유저 강퇴")
    class BanUser {
        @Test
        @DisplayName("방 id가 잘못된 경우")
        void invalidRoomId() {
            // given
            when(roomRepository.findByIdWithParticipants(2L)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> roomService.banUser(2L, 1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("방장 id가 잘못된 경우")
        void invalidManagerId() {
            // given
            room.acceptParticipant(participant);
            room.acceptParticipant(newbie);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.banUser(1L, 2L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("유저 id가 잘못된 경우")
        void invalidUserId() {
            // given
            room.acceptParticipant(participant);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(4L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.banUser(1L, 1L, 4L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("강퇴 대상이 방에 없는 경우")
        void banUser_NotParticipant() {
            // given
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

            // when & then
            assertThatThrownBy(() -> roomService.banUser(1L, 1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }

        @Test
        @DisplayName("성공")
        void success() {
            // given
            room.acceptParticipant(participant);
            when(roomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(room));
            when(userRepository.findById(2L)).thenReturn(Optional.of(participant));

            // when
            roomService.banUser(1L, 1L, 2L);

            // then
            assertThat(room.getParticipants()).hasSize(1);
            assertThat(room.getParticipants()).doesNotContain(participant);
            verify(messageBrokerChatService, times(1)).sendNoticeToRedisStreams(1L, participant.getNickname(), MessageType.BAN);
            verify(redisService, times(1)).deleteUserCurrentRoomId(2L);
        }
    }
}