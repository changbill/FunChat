package com.funchat.demo.room.service;

import com.funchat.demo.chat.service.ChatFanoutBroker;
import com.funchat.demo.chat.service.ChatPersistBroker;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.room.domain.dto.RoomRequest;
import com.funchat.demo.room.domain.dto.RoomResponse;
import com.funchat.demo.room.domain.dto.RoomUpdateRequest;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ChatPersistBroker chatPersistBroker;

    @MockitoBean
    private ChatFanoutBroker chatFanoutBroker;

    @Nested
    @DisplayName("방 만들기")
    class CreateRoom {

        @Test
        @DisplayName("성공")
        void whenUserExists_thenCreateRoomWithManager() {
            // given
            User creator = saveUser("creator@test.com", "creator");
            RoomRequest request = new RoomRequest("새로운 방", 10);

            // when
            RoomResponse response = roomService.createRoom(request, creator.getId());

            // then
            Room room = roomRepository.findById(response.roomId()).orElseThrow();
            assertThat(response.title()).isEqualTo("새로운 방");
            assertThat(response.maxMembers()).isEqualTo(10);
            assertThat(response.currentMembers()).isEqualTo(1);
            assertThat(room.getManager()).isEqualTo(creator);
            assertThat(creator.getRoom()).isEqualTo(room);
        }
    }

    @Nested
    @DisplayName("방 정보 수정")
    class UpdateRoom {

        @Test
        @DisplayName("성공")
        void whenManagerUpdatesRoom_thenReturnUpdatedRoom() {
            // given
            TestRoom testRoom = saveRoom("수정 전", 5);
            RoomUpdateRequest request = new RoomUpdateRequest("수정된 방 제목", 10);

            // when
            RoomResponse response = roomService.updateRoom(testRoom.room.getId(), request, testRoom.manager.getId());

            // then
            assertThat(response.title()).isEqualTo("수정된 방 제목");
            assertThat(response.maxMembers()).isEqualTo(10);
        }

        @Test
        @DisplayName("수정 요청자가 매니저가 아니면 실패")
        void whenUserIsNotManager_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);
            RoomUpdateRequest request = new RoomUpdateRequest("수정", 5);

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(testRoom.room.getId(), request, participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("최대 인원을 현재 인원보다 작게 줄이면 실패")
        void whenMaxMembersIsLessThanCurrentMembers_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            enterParticipant(testRoom.room, saveUser("p1@test.com", "p1"));
            enterParticipant(testRoom.room, saveUser("p2@test.com", "p2"));
            RoomUpdateRequest request = new RoomUpdateRequest("수정", 2);

            // when & then
            assertThatThrownBy(() -> roomService.updateRoom(testRoom.room.getId(), request, testRoom.manager.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("방장 위임")
    class DelegateManager {

        @Test
        @DisplayName("성공")
        void whenTargetIsParticipant_thenDelegateManager() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when
            roomService.delegateManager(testRoom.room.getId(), testRoom.manager.getId(), participant.getId());

            // then
            assertThat(testRoom.room.getManager()).isEqualTo(participant);
        }

        @Test
        @DisplayName("현재 매니저가 존재하지 않으면 실패")
        void whenManagerDoesNotExist_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);

            // when & then
            assertThatThrownBy(() -> roomService.delegateManager(testRoom.room.getId(), 999L, testRoom.manager.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("요청자가 매니저가 아니면 실패")
        void whenUserIsNotManager_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            User newbie = saveUser("newbie@test.com", "newbie");
            enterParticipant(testRoom.room, participant);
            enterParticipant(testRoom.room, newbie);

            // when & then
            assertThatThrownBy(() -> roomService.delegateManager(testRoom.room.getId(), participant.getId(), newbie.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("위임 대상이 방 참여자가 아니면 실패")
        void whenTargetIsNotParticipant_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User newbie = saveUser("newbie@test.com", "newbie");

            // when & then
            assertThatThrownBy(() -> roomService.delegateManager(testRoom.room.getId(), testRoom.manager.getId(), newbie.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }
    }

    @Nested
    @DisplayName("방 삭제")
    class DeleteRoom {

        @Test
        @DisplayName("성공")
        void whenManagerDeletesRoom_thenRemoveRoomAndRelations() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);
            Long roomId = testRoom.room.getId();

            // when
            roomService.deleteRoom(roomId, testRoom.manager.getId());
            roomRepository.flush();

            // then
            assertThat(roomRepository.findById(roomId)).isEmpty();
            assertThat(participant.getRoom()).isNull();
        }

        @Test
        @DisplayName("요청자가 매니저가 아니면 실패")
        void whenUserIsNotManager_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when & then
            assertThatThrownBy(() -> roomService.deleteRoom(testRoom.room.getId(), participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }
    }

    @Nested
    @DisplayName("방 입장")
    class EnterRoom {

        @Test
        @DisplayName("성공")
        void whenRoomHasCapacity_thenEnterRoom() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");

            // when
            RoomResponse response = roomService.enterRoom(testRoom.room.getId(), participant.getId());

            // then
            assertThat(response.currentMembers()).isEqualTo(2);
            assertThat(participant.getRoom()).isEqualTo(testRoom.room);
            assertThat(testRoom.room.getParticipants()).contains(participant);
        }

        @Test
        @DisplayName("방 ID가 존재하지 않으면 실패")
        void whenRoomDoesNotExist_thenThrowException() {
            // given
            User user = saveUser("participant@test.com", "participant");

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(999L, user.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 실패")
        void whenUserDoesNotExist_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(testRoom.room.getId(), 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("강퇴된 사용자는 실패")
        void whenUserIsBanned_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            testRoom.room.setBannedUsers(participant);

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(testRoom.room.getId(), participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_BANNED);
        }

        @Test
        @DisplayName("정원이 가득 차면 실패")
        void whenRoomIsFull_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 2);
            User participant = saveUser("participant@test.com", "participant");
            User newbie = saveUser("newbie@test.com", "newbie");
            enterParticipant(testRoom.room, participant);

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(testRoom.room.getId(), newbie.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_MAX_CAPACITY_REACHED);
        }

        @Test
        @DisplayName("이미 채팅방에 참여 중이면 실패")
        void whenUserAlreadyJoinedRoom_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when & then
            assertThatThrownBy(() -> roomService.enterRoom(testRoom.room.getId(), participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_ALREADY_JOINED);
        }
    }

    @Nested
    @DisplayName("방 나가기")
    class LeaveRoom {

        @Test
        @DisplayName("일반 참여자가 나가면 방은 유지되고 참여자 명단에서 제거된다")
        void whenParticipantLeaves_thenKeepRoomAndRemoveParticipant() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when
            roomService.leaveRoom(participant.getId());

            // then
            assertThat(roomRepository.findById(testRoom.room.getId())).isPresent();
            assertThat(participant.getRoom()).isNull();
            assertThat(testRoom.room.getParticipants()).containsOnly(testRoom.manager);
        }

        @Test
        @DisplayName("방장이 나가면 다음 참여자에게 방장이 위임된다")
        void whenManagerLeavesWithParticipant_thenDelegateManager() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when
            roomService.leaveRoom(testRoom.manager.getId());

            // then
            assertThat(testRoom.room.getManager()).isEqualTo(participant);
            assertThat(testRoom.room.getParticipants()).containsOnly(participant);
        }

        @Test
        @DisplayName("마지막 참여자가 나가면 방이 삭제된다")
        void whenLastParticipantLeaves_thenDeleteRoom() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            Long roomId = testRoom.room.getId();

            // when
            roomService.leaveRoom(testRoom.manager.getId());
            roomRepository.flush();

            // then
            assertThat(roomRepository.findById(roomId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("유저 강퇴")
    class BanUser {

        @Test
        @DisplayName("성공")
        void whenTargetIsParticipant_thenBanAndRemoveUser() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            enterParticipant(testRoom.room, participant);

            // when
            roomService.banUser(testRoom.room.getId(), testRoom.manager.getId(), participant.getId());

            // then
            assertThat(testRoom.room.getBannedUserIds()).contains(participant.getId());
            assertThat(participant.getRoom()).isNull();
            assertThat(testRoom.room.getParticipants()).containsOnly(testRoom.manager);
        }

        @Test
        @DisplayName("방 ID가 존재하지 않으면 실패")
        void whenRoomDoesNotExist_thenThrowException() {
            // given
            User manager = saveUser("manager@test.com", "manager");
            User participant = saveUser("participant@test.com", "participant");

            // when & then
            assertThatThrownBy(() -> roomService.banUser(999L, manager.getId(), participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("요청자가 매니저가 아니면 실패")
        void whenUserIsNotManager_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");
            User newbie = saveUser("newbie@test.com", "newbie");
            enterParticipant(testRoom.room, participant);
            enterParticipant(testRoom.room, newbie);

            // when & then
            assertThatThrownBy(() -> roomService.banUser(testRoom.room.getId(), participant.getId(), newbie.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_MANAGER);
        }

        @Test
        @DisplayName("강퇴 대상이 존재하지 않으면 실패")
        void whenTargetDoesNotExist_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);

            // when & then
            assertThatThrownBy(() -> roomService.banUser(testRoom.room.getId(), testRoom.manager.getId(), 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("강퇴 대상이 방 참여자가 아니면 실패")
        void whenTargetIsNotParticipant_thenThrowException() {
            // given
            TestRoom testRoom = saveRoom("테스트 방", 5);
            User participant = saveUser("participant@test.com", "participant");

            // when & then
            assertThatThrownBy(() -> roomService.banUser(testRoom.room.getId(), testRoom.manager.getId(), participant.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }
    }

    private User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.createUser(email, "password", nickname, ""));
    }

    private TestRoom saveRoom(String title, int maxMembers) {
        User manager = saveUser("manager-" + System.nanoTime() + "@test.com", "manager" + System.nanoTime());
        Room room = roomRepository.saveAndFlush(Room.createRoom(title, maxMembers, manager));
        return new TestRoom(room, manager);
    }

    private void enterParticipant(Room room, User user) {
        long currentCount = userRepository.countByRoomId(room.getId());
        room.acceptParticipant(user, currentCount);
        roomRepository.flush();
    }

    private record TestRoom(Room room, User manager) {
    }
}
