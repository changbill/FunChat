package com.funchat.demo.room.domain;

import com.funchat.demo.global.config.JpaAuditingConfig;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
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
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("채팅방과 매니저/참여자 관계를 저장하고 조회한다")
    void saveAndFindRoom() {
        User manager = userRepository.saveAndFlush(User.createUser("manager@test.com", "password", "manager", ""));
        Room room = roomRepository.saveAndFlush(Room.createRoom("test room", 10, manager));

        assertThat(roomRepository.findById(room.getId()))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getTitle()).isEqualTo("test room");
                    assertThat(found.getManager()).isEqualTo(manager);
                    assertThat(found.getParticipants()).containsExactly(manager);
                });
    }
}
