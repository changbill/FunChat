//package com.funchat.demo.global.dummy.test;
//
//import com.funchat.demo.global.dummy.service.DummyDataService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class DummyDataInitRunner implements CommandLineRunner {
//
//    private final DummyDataService dummyDataService;
//
//    @Override
//    public void run(String... args) {
//        // 실행 시 로그를 통해 진행 상황을 확인하세요.
//        dummyDataService.bulkInsert();
//    }
//}