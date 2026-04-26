package com.funchat.demo.user.domain;

import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("이메일과 닉네임 중복 여부를 조회한다")
    void existsByEmailAndNickname() {
        User user = User.createUser("user@test.com", "password", "tester", "");
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("user@test.com")).isTrue();
        assertThat(userRepository.existsByNickname("tester")).isTrue();
        assertThat(userRepository.findByEmail("user@test.com")).contains(user);
    }

    @Test
    @DisplayName("채팅방 참여자 수와 참여 여부를 roomId 기준으로 조회한다")
    void roomParticipantQueries() {
        User manager = userRepository.saveAndFlush(User.createUser("manager@test.com", "password", "manager", ""));
        Room room = roomRepository.saveAndFlush(Room.createRoom("room", 5, manager));

        assertThat(userRepository.countByRoomId(room.getId())).isEqualTo(1);
        assertThat(userRepository.findByRoomId(room.getId())).containsExactly(manager);
        assertThat(userRepository.existsByIdAndRoom_Id(manager.getId(), room.getId())).isTrue();
        assertThat(userRepository.existsByIdAndRoomIsNotNull(manager.getId())).isTrue();
    }

    @Test
    @DisplayName("방장 외 첫 번째 참여자를 조회한다")
    void findFirstByRoomIdAndIdNot() {
        User manager = userRepository.saveAndFlush(User.createUser("manager@test.com", "password", "manager", ""));
        Room room = roomRepository.saveAndFlush(Room.createRoom("room", 5, manager));
        User participant = userRepository.saveAndFlush(User.createUser("user@test.com", "password", "participant", ""));
        room.acceptParticipant(participant, 1);
        roomRepository.flush();

        assertThat(userRepository.findFirstByRoomIdAndIdNot(room.getId(), manager.getId()))
                .contains(participant);
    }
}
