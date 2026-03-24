package com.funchat.demo.user.service;

import com.funchat.demo.auth.domain.dto.TokenResponse;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import com.funchat.demo.user.domain.dto.LoginRequest;
import com.funchat.demo.user.domain.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createForTest(1L, "email", "nickname", "test.png");
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(userService, "refreshExpiration", 3600000L);   // value 값 들어갈 수 있도록
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {
        @Test
        @DisplayName("회원가입 성공 - 중복된 이메일과 닉네임이 없어야 한다")
        void signUp_Success() {
            // given
            SignUpRequest request = new SignUpRequest("email1", "password", "nickname2");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

            // when
            userService.signUp(request);

            // then
            verify(userRepository, times(1)).save(any(User.class));
            verify(passwordEncoder).encode("password");
        }

        @Test
        @DisplayName("회원가입 실패 - 이미 존재하는 이메일이면 예외가 발생한다")
        void signUp_Fail_EmailExists() {
            // given
            SignUpRequest request = new SignUpRequest("email", "password", "테스트유저");
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAILS_ALREADY_EXIST);
        }

        @Test
        @DisplayName("회원가입 실패 - 이미 존재하는 닉네임이면 예외가 발생")
        void signUp_Fail_NicknameExists() {
            // given
            SignUpRequest request = new SignUpRequest("test@test.com", "password", "nickname");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByNickname(anyString())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXIST);
        }
    }

    @Nested
    @DisplayName("로그인")
    class login {
        @Test
        @DisplayName("로그인 성공 - 토큰을 반환하고 리프레시 토큰을 Redis에 저장한다")
        void login_Success() {
            // given
            LoginRequest request = new LoginRequest("email", "password");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.createAccessToken(anyString())).thenReturn("access-token");
            when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh-token");

            // when
            TokenResponse response = userService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.nickname()).isEqualTo("nickname");
            verify(redisService).saveRefreshToken(eq("email"), eq("refresh-token"), any(Duration.class));
        }

        @Test
        @DisplayName("이메일을 발견하지 못한 경우")
        void cantFindEmail() {
            // given
            LoginRequest request = new LoginRequest("email1", "password");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_FOUND);
        }

        @Test
        @DisplayName("인코딩한 비밀번호가 일치하지 않는 경우")
        void passwordDoesntMatch() {
            // given
            LoginRequest request = new LoginRequest("email", "password1");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password1", "password")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class reissue {
        @Test
        @DisplayName("토큰 재발급 성공 - 저장된 리프레시 토큰과 일치하면 새 토큰을 발급한다")
        void reissue_Success() {
            // given
            String refreshToken = "Bearer valid-refresh";
            String pureToken = "valid-refresh";

            when(jwtTokenProvider.getEmail("valid-refresh")).thenReturn("test@test.com");
            when(redisService.getRefreshToken("test@test.com")).thenReturn(pureToken);
            when(jwtTokenProvider.createAccessToken("test@test.com")).thenReturn("new-access");
            when(jwtTokenProvider.createRefreshToken("test@test.com")).thenReturn("new-refresh");

            // when
            TokenResponse response = userService.reissue(refreshToken);

            // then
            verify(jwtTokenProvider, times(1)).validateRefreshToken(any(Optional.class));
            assertThat(response.accessToken()).isEqualTo("new-access");
            assertThat(response.refreshToken()).isEqualTo("new-refresh");
            verify(redisService).saveRefreshToken(eq("test@test.com"), eq("new-refresh"), any(Duration.class));
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 리프레시 토큰이 탈취되어 이미 이전에 공격자가 재발급 받은 경우")
        void reissue_refreshToken_not_matches() {
            // given
            String refreshToken = "Bearer valid-refresh";
            String pureToken = "valid-refresh"; // 파싱 후 나올 실제 토큰 값
            String stolenToken = "attacker-stolen-token";

            when(jwtTokenProvider.getEmail(pureToken)).thenReturn("test@test.com");
            when(redisService.getRefreshToken("test@test.com")).thenReturn(stolenToken);

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

            verify(redisService).deleteRefreshToken("test@test.com");
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 리프레시 토큰이 탈취되어 이전에 재발급 취소되어 지워짐")
        void reissue_refreshToken_is_deleted() {
            // given
            String refreshToken = "Bearer valid-refresh";
            String pureToken = "valid-refresh";

            when(jwtTokenProvider.getEmail(pureToken)).thenReturn("test@test.com");
            when(redisService.getRefreshToken("test@test.com")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> userService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

            verify(redisService).deleteRefreshToken("test@test.com");
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class logout {

        @Test
        @DisplayName("로그아웃 성공 - 액세스 토큰을 블랙리스트에 등록하고 리프레시 토큰을 삭제한다")
        void logout_Success() {
            // given
            String accessToken = "Bearer access-token";
            when(jwtTokenProvider.getExpiration(anyString())).thenReturn(1000L);
            when(jwtTokenProvider.getEmail(anyString())).thenReturn("test@test.com");

            // when
            userService.logout(accessToken);

            // then
            verify(jwtTokenProvider).validateAccessToken(any(Optional.class));
            verify(redisService).saveBlacklist(anyString(), eq("logout"), any(Duration.class));
            verify(redisService).deleteRefreshToken("test@test.com");
        }
    }
}
