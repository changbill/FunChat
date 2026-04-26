package com.funchat.demo.user.service;

import com.funchat.demo.chat.service.ChatFanoutBroker;
import com.funchat.demo.chat.service.ChatPersistBroker;
import com.funchat.demo.auth.domain.dto.TokenResponse;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private ChatPersistBroker chatPersistBroker;

    @MockitoBean
    private ChatFanoutBroker chatFanoutBroker;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("성공")
        void whenUniqueEmailAndNickname_thenSaveEncodedPassword() {
            // given
            SignUpRequest request = new SignUpRequest("signup@test.com", "password", "signup");

            // when
            userService.signUp(request);

            // then
            User saved = userRepository.findByEmail("signup@test.com").orElseThrow();
            assertThat(saved.getNickname()).isEqualTo("signup");
            assertThat(passwordEncoder.matches("password", saved.getPassword())).isTrue();
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 실패")
        void whenEmailAlreadyExists_thenThrowException() {
            // given
            userRepository.save(User.createUser("duplicate@test.com", "password", "user1", ""));
            SignUpRequest request = new SignUpRequest("duplicate@test.com", "password", "user2");

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAILS_ALREADY_EXIST);
        }

        @Test
        @DisplayName("이미 존재하는 닉네임이면 실패")
        void whenNicknameAlreadyExists_thenThrowException() {
            // given
            userRepository.save(User.createUser("user1@test.com", "password", "duplicate", ""));
            SignUpRequest request = new SignUpRequest("user2@test.com", "password", "duplicate");

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXIST);
        }

        @Test
        @DisplayName("이메일이 비어 있으면 실패")
        void whenEmailIsBlank_thenThrowException() {
            // given
            SignUpRequest request = new SignUpRequest("", "password", "nickname");

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공")
        void whenCredentialsAreValid_thenReturnTokensAndSaveRefreshToken() {
            // given
            userRepository.save(User.createUser(
                    "login@test.com",
                    passwordEncoder.encode("password"),
                    "login",
                    ""
            ));
            LoginRequest request = new LoginRequest("login@test.com", "password");

            // when
            TokenResponse response = userService.login(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.nickname()).isEqualTo("login");
            verify(redisService).saveRefreshToken(any(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 실패")
        void whenEmailDoesNotExist_thenThrowException() {
            // given
            LoginRequest request = new LoginRequest("missing@test.com", "password");

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_FOUND);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 실패")
        void whenPasswordDoesNotMatch_thenThrowException() {
            // given
            userRepository.save(User.createUser(
                    "wrong-password@test.com",
                    passwordEncoder.encode("password"),
                    "wrongPassword",
                    ""
            ));
            LoginRequest request = new LoginRequest("wrong-password@test.com", "invalid");

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("성공")
        void whenStoredRefreshTokenMatches_thenIssueNewTokens() {
            // given
            String email = "reissue@test.com";
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            when(redisService.getRefreshToken(email)).thenReturn(refreshToken);

            // when
            TokenResponse response = userService.reissue("Bearer " + refreshToken);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            verify(redisService).saveRefreshToken(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("저장된 리프레시 토큰과 다르면 삭제 후 실패")
        void whenStoredRefreshTokenDoesNotMatch_thenDeleteTokenAndThrowException() {
            // given
            String email = "reissue@test.com";
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            when(redisService.getRefreshToken(email)).thenReturn("stolen-token");

            // when & then
            assertThatThrownBy(() -> userService.reissue("Bearer " + refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
            verify(redisService).deleteRefreshToken(email);
        }

        @Test
        @DisplayName("저장된 리프레시 토큰이 없으면 실패")
        void whenStoredRefreshTokenIsMissing_thenThrowException() {
            // given
            String email = "reissue@test.com";
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            when(redisService.getRefreshToken(email)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> userService.reissue("Bearer " + refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
            verify(redisService).deleteRefreshToken(email);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("성공")
        void whenAccessTokenIsValid_thenBlacklistAccessTokenAndDeleteRefreshToken() {
            // given
            String email = "reissue@test.com";
            String accessToken = jwtTokenProvider.createAccessToken(email);

            // when
            userService.logout("Bearer " + accessToken);

            // then
            verify(redisService).saveBlacklist(any(), any(), any(Duration.class));
            verify(redisService).deleteRefreshToken(email);
        }
    }
}
