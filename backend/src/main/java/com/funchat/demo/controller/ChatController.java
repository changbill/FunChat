package com.funchat.demo.controller;

import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/chat")
public class ChatController {

    // 테스트를 위해 DB 대신 메모리에 방 정보를 저장합니다.
    private final Map<UUID, GroupRoom> roomMap = new ConcurrentHashMap<>();

    // 1. 방 만들기 폼 페이지 (단순 이동)
    @GetMapping("/room/form")
    public String roomForm() {
        return "room/form";
    }

    // 2. 방 생성 처리
    @PostMapping("/room/group/create")
    public String createGroupRoom(@RequestParam String roomName) {
        UUID roomId = UUID.randomUUID();
        GroupRoom newRoom = new GroupRoom(roomId, roomName);
        roomMap.put(roomId, newRoom);

        // 생성 후 상세 페이지로 리다이렉트
        return "redirect:/chat/room/group/" + roomId;
    }

    // 3. 채팅방 상세 페이지 (여기서 웹소켓 연결)
    @GetMapping("/room/group/{roomId}")
    public String getGroupRoom(@PathVariable UUID roomId, Model model) {
        GroupRoom room = roomMap.get(roomId);

        if (room == null) {
            return "redirect:/chat/room/form"; // 방이 없으면 폼으로
        }

        model.addAttribute("room", room);
        return "chat/room"; // resources/templates/chat/room.html
    }

    // 테스트용 내부 클래스 (DTO)
    @Data
    public static class GroupRoom {
        private UUID roomId;
        private String roomName;

        public GroupRoom(UUID roomId, String roomName) {
            this.roomId = roomId;
            this.roomName = roomName;
        }
    }
}