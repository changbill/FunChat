package com.funchat.demo.global.dummy.service;

import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final Faker faker = new Faker(new Locale("ko"));

    @Transactional
    public void bulkInsert() {
        log.info("=== 더미 데이터 삽입 시작 (Unique 제약 준수 버전) ===");

        // 100개씩 끊어서 50번 반복 = 총 5,000 세트 (유저 5,000 + 방 5,000)
        for (int i = 0; i < 50; i++) {
            List<User> users = new ArrayList<>();
            List<Room> rooms = new ArrayList<>();

            for (int j = 0; j < 100; j++) {
                int index = (i * 100) + j;

                // 1. 유저 생성
                User user = User.createUser(
                        "test" + index + "@funchat.com",
                        "password123!",
                        faker.funnyName().name() + index,
                        "https://picsum.photos/200?random=" + index
                );
                users.add(user);

                // 2. 방 생성 (현재 생성한 유저를 바로 방장으로 임명 - 중복 원천 차단)
                Room room = Room.createRoom(
                        faker.book().title() + " " + index + "번방",
                        faker.number().numberBetween(10, 100),
                        user
                );
                rooms.add(room);
            }

            // 유저를 먼저 저장해야 Room에서 manager_id 참조가 가능합니다.
            userRepository.saveAll(users);
            roomRepository.saveAll(rooms);

            log.info("데이터 생성 중... 현재 {} 세트 완료", (i + 1) * 100);
        }

        log.info("=== 삽입 완료 (유저 5,000명, 방 5,000개) ===");
    }
}