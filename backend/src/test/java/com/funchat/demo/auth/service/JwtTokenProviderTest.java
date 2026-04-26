package com.funchat.demo.auth.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "v3eryasdafwfqefqefacaseAspdfiqjwfiqejfpjasfipaefasfasfaefqfdqdqwdencoded=";
    private final long accessExpiration = 10000; // 10초
    private final long refreshExpiration = 60000; // 60초
    private final String email = "test@test.com";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, accessExpiration, refreshExpiration);
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 이메일 추출 성공")
    void createAccessToken_Success() {
        // when
        String token = jwtTokenProvider.createAccessToken(email);
        String extractedEmail = jwtTokenProvider.getEmail(token);

        // then
        assertThat(token).isNotNull();
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Nested
    @DisplayName("accessToken 검증")
    class AccessTokenValidation {

        @Test
        @DisplayName("유효한 액세스 토큰은 검증을 통과한다")
        void success() {
            String token = jwtTokenProvider.createAccessToken(email);
            assertDoesNotThrow(() -> jwtTokenProvider.validateAccessToken(Optional.of(token)));
        }

        @Test
        @DisplayName("액세스 토큰 자리에 리프레시 토큰이 들어오면 INVALID 에러가 발생한다")
        void fail_when_refreshToken_given() {
            String refreshToken = jwtTokenProvider.createRefreshToken(email);

            assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(Optional.of(refreshToken)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("빈 토큰(Optional.empty)이 들어오면 NOT_FOUND 에러가 발생한다")
        void fail_when_empty() {
            assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(Optional.empty()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("만료 기간이 지난 엑세스 토큰은 EXPIRED 에러가 발생한다")
        void fail_expired() {
            JwtTokenProvider jwtTokenProvider2 = new JwtTokenProvider(secretKey, 1, 1);
            String expiredToken = jwtTokenProvider2.createAccessToken(email);

            assertThatThrownBy(() -> jwtTokenProvider2.validateAccessToken(Optional.of(expiredToken)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_ACCESS_TOKEN);
        }
    }

    @Nested
    @DisplayName("refreshToken 검증")
    class RefreshTokenValidation {

        @Test
        @DisplayName("유효한 리프레시 토큰은 검증을 통과한다")
        void success() {
            String token = jwtTokenProvider.createRefreshToken(email);
            assertDoesNotThrow(() -> jwtTokenProvider.validateRefreshToken(Optional.of(token)));
        }

        @Test
        @DisplayName("리프레시 토큰 자리에 액세스 토큰이 들어오면 INVALID 에러가 발생한다")
        void fail_when_accessToken_given() {
            String accessToken = jwtTokenProvider.createAccessToken(email);

            assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(Optional.of(accessToken)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("변조된 토큰이 들어오면 INVALID 에러가 발생한다")
        void fail_when_tampered() {
            String validToken = jwtTokenProvider.createRefreshToken(email);
            String tamperedToken = validToken + "wrong";

            assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(Optional.of(tamperedToken)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰을 검증하면 EXPIRED 에러가 발생한다")
        void validate_fail_expired() {
            // given
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secretKey, 1, 1);
            String expiredToken = shortLivedProvider.createRefreshToken(email);

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(Optional.of(expiredToken)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_REFRESH_TOKEN);
        }
    }


}
