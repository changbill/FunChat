package com.funchat.demo.chat;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.UserRepository;
import com.funchat.demo.util.ParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            ensureAuthenticated(accessor);
            String destination = accessor.getDestination(); // 예: /sub/chat/123
            if (destination != null && destination.startsWith("/sub/chat/")) {
                Long roomId = ParseUtil.parseLong(
                        destination.replace("/sub/chat/", ""),
                        new BusinessException(ErrorCode.ROOM_NOT_FOUND)
                );

                validateUserInRoom(accessor, roomId);
            }
        }

        if(StompCommand.SEND.equals(command)) {
            ensureAuthenticated(accessor);
            Optional<String> roomIdStr = Optional.ofNullable(accessor.getFirstNativeHeader("roomId"));
            Long roomId = ParseUtil.parseLong(roomIdStr, new BusinessException(ErrorCode.ROOM_NOT_FOUND));
            validateUserInRoom(accessor, roomId);
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private void ensureAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            authenticate(accessor);
        }
    }

    private void authenticate(StompHeaderAccessor accessor) {
        Optional<String> token = extractToken(accessor);
        jwtTokenProvider.validateAccessToken(token);
        String accessToken = token.get();

        if (redisService.isBlacklisted(accessToken)) {
            throw new BusinessException(ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
    }

    private void validateUserInRoom(StompHeaderAccessor accessor, Long roomId) {
        CustomUserDetails userDetails = getCustomUserDetails(accessor);
        Long userId = userDetails.user().getId();

        if (!userRepository.existsByIdAndRoom_Id(userId, roomId)) {
            throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
        }
    }

    private CustomUserDetails getCustomUserDetails(StompHeaderAccessor accessor) {
        return Optional.ofNullable(accessor.getUser())
                .filter(Authentication.class::isInstance)
                .map(Authentication.class::cast)
                .map(Authentication::getPrincipal)
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Optional<String> extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        return AuthUtil.resolveToken(bearerToken);
    }
}