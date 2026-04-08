package com.funchat.demo.user.service;

import com.funchat.demo.auth.domain.dto.TokenResponse;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import com.funchat.demo.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public void signUp(SignUpRequest request) {
        String email = request.email();
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String nickname = request.nickname();
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAILS_ALREADY_EXIST);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXIST);
        }

        // 비밀번호 암호화 및 유저 저장
        User user = User.createUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                ""
        );

        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String email = user.getEmail();
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        redisService.saveRefreshToken(email, refreshToken, Duration.ofMillis(refreshExpiration));
        return TokenResponse.loginOf(accessToken, refreshToken, user.getNickname());
    }

    public TokenResponse reissue(String refreshToken) {
        Optional<String> optionalToken = AuthUtil.resolveToken(refreshToken);
        jwtTokenProvider.validateRefreshToken(optionalToken);

        String token = optionalToken.get();
        String email = jwtTokenProvider.getEmail(token);
        String savedRefreshToken = redisService.getRefreshToken(email);
        // 저장된 리프레시 토큰이 다른 경우 탈취되어 이미 이전에 공격자가 재발급 받은 것
        // 저장된 리프레시 토큰이 없는 경우 위의 경우에서 다시 재발급을 시도한 것
        if (savedRefreshToken == null || !savedRefreshToken.equals(token)) {
            redisService.deleteRefreshToken(email);    // 공격 시도로 강제 로그아웃 처리
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);
        redisService.saveRefreshToken(email, newRefreshToken, Duration.ofMillis(refreshExpiration));
        return TokenResponse.reissueOf(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String accessToken) {
        Optional<String> optionalToken = AuthUtil.resolveToken(accessToken);
        jwtTokenProvider.validateAccessToken(optionalToken);

        String token = optionalToken.get();
        Long expiration = jwtTokenProvider.getExpiration(token);
        redisService.saveBlacklist(token, "logout", Duration.ofMillis(expiration));

        String email = jwtTokenProvider.getEmail(token);
        redisService.deleteRefreshToken(email);
    }
}