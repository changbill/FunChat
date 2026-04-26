package com.funchat.demo.chat;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RedisService redisService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private StompHandler stompHandler;

    @Test
    @DisplayName("CONNECT는 Authorization 헤더의 access token으로 사용자를 인증한다")
    void connect_AuthenticatesUser() {
        // given
        User user = User.createForTest(1L, "user@test.com", "tester", "");
        when(redisService.isBlacklisted("access-token")).thenReturn(false);
        when(jwtTokenProvider.getEmail("access-token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));

        Message<?> message = stompMessage(StompCommand.CONNECT, null, "Bearer access-token", null, null);

        // when
        stompHandler.preSend(message, messageChannel);

        // then
        verify(jwtTokenProvider).validateAccessToken(Optional.of("access-token"));
        verify(userDetailsService).loadUserByUsername("user@test.com");
    }

    @Test
    @DisplayName("SUBSCRIBE는 채팅방 참여자가 아니면 거부한다")
    void subscribe_RejectsNonParticipant() {
        // given
        User user = User.createForTest(1L, "user@test.com", "tester", "");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/sub/chat/10",
                null,
                null,
                authenticated(userDetails)
        );

        when(userRepository.existsByIdAndRoom_Id(1L, 10L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
    }

    @Test
    @DisplayName("SUBSCRIBE는 채팅방 참여자이면 허용한다")
    void subscribe_AllowsParticipant() {
        // given
        User user = User.createForTest(1L, "user@test.com", "tester", "");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/sub/chat/10",
                null,
                null,
                authenticated(userDetails)
        );

        when(userRepository.existsByIdAndRoom_Id(1L, 10L)).thenReturn(true);

        // when
        stompHandler.preSend(message, messageChannel);

        // then
        verify(userRepository).existsByIdAndRoom_Id(1L, 10L);
    }

    @Test
    @DisplayName("SEND는 roomId native header가 없으면 거부한다")
    void send_RejectsMissingRoomId() {
        // given
        User user = User.createForTest(1L, "user@test.com", "tester", "");
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/pub/chat/message",
                null,
                null,
                authenticated(new CustomUserDetails(user))
        );

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("SEND는 채팅방 참여자가 아니면 거부한다")
    void send_RejectsNonParticipant() {
        // given
        User user = User.createForTest(1L, "user@test.com", "tester", "");
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/pub/chat/message",
                null,
                "10",
                authenticated(new CustomUserDetails(user))
        );

        when(userRepository.existsByIdAndRoom_Id(1L, 10L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_USER_NOT_PARTICIPANT);
    }

    @Test
    @DisplayName("CONNECT는 블랙리스트된 access token이면 거부한다")
    void connect_RejectsBlacklistedAccessToken() {
        // given
        when(redisService.isBlacklisted("access-token")).thenReturn(true);
        Message<?> message = stompMessage(StompCommand.CONNECT, null, "Bearer access-token", null, null);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);

        verify(jwtTokenProvider).validateAccessToken(Optional.of("access-token"));
    }

    private Message<?> stompMessage(
            StompCommand command,
            String destination,
            String authorization,
            String roomId,
            UsernamePasswordAuthenticationToken user
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);

        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }

        if (roomId != null) {
            accessor.addNativeHeader("roomId", roomId);
        }

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authenticated(CustomUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
