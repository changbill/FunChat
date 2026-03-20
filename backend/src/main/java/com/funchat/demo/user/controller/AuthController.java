package com.funchat.demo.user.controller;

import com.funchat.demo.global.dto.ResponseDto;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import com.funchat.demo.user.service.UserService;
import com.funchat.demo.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ResponseDto> signUp(@RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseUtil.createSuccessResponse(null);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody LoginRequest request) {
        return ResponseUtil.createSuccessResponse(userService.login(request));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ResponseDto> reissue(
            @RequestHeader("Authorization-Refresh") String bearerToken) {
        return ResponseUtil.createSuccessResponse(userService.reissue(bearerToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDto> logout(@RequestHeader("Authorization") String bearerToken) {
        userService.logout(bearerToken);
        return ResponseUtil.createSuccessResponse(null);
    }
}