package com.funchat.demo.user.service;

import com.funchat.demo.auth.domain.dto.TokenResponse;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.SocialType;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserStatus;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import com.funchat.demo.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public void signUp(SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAILS_ALREADY_EXIST);
        }

        if(userRepository.existsByNickname(request.nickname())){
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXIST);
        }

        // 비밀번호 암호화 및 유저 저장
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .status(UserStatus.OFFLINE)
                .socialType(SocialType.LOCAL)
                .build();

        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(request.email());
        String refreshToken = jwtTokenProvider.createRefreshToken(request.email());

        redisTemplate.opsForValue().set(
                "RT:" + user.getEmail(),
                refreshToken,
                Duration.ofMillis(refreshExpiration)
        );

        return TokenResponse.loginOf(accessToken, refreshToken, user.getNickname());
    }

    public TokenResponse reissue(String refreshToken) {
        Optional<String> OptionalToken = AuthUtil.resolveToken(refreshToken);
        jwtTokenProvider.validateRefreshToken(OptionalToken);

        String token = OptionalToken.get();
        String email = jwtTokenProvider.getEmail(token);
        String savedRefreshToken = (String) redisTemplate.opsForValue().get("RT:" + email);
        if (savedRefreshToken == null || !savedRefreshToken.equals(token)) {
            redisTemplate.delete("RT:" + email);    // 공격 시도로 강제 로그아웃 처리
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email); // 발급 메서드 필요

        redisTemplate.opsForValue().set("RT:" + email, newRefreshToken, Duration.ofMillis(refreshExpiration));

        return TokenResponse.reissueOf(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String accessToken) {
        Optional<String> OptionalToken = AuthUtil.resolveToken(accessToken);
        jwtTokenProvider.validateAccessToken(OptionalToken);

        String token = OptionalToken.get();
        Long expiration = jwtTokenProvider.getExpiration(token);
        redisTemplate.opsForValue()
                .set("blacklist:" + token, "logout", Duration.ofMillis(expiration));

        String email = jwtTokenProvider.getEmail(token);
        redisTemplate.delete("RT:" + email);
    }
}