package com.funchat.demo.chat;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import com.funchat.demo.room.domain.RoomRepository;
import com.funchat.demo.user.domain.User;
import com.funchat.demo.user.domain.UserRepository;
import com.funchat.demo.util.ParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    /**
     * 기대 행동: 메시지를 보내기 전에 가공하거나 검사한다.
     * 반환 값: Message<?>를 반환해야 하며, null을 반환하면 메시지 전송을 중단한다.
     * 예외: 명시적인 throws 절이 없다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            Optional<String> token = extractToken(accessor);
            jwtTokenProvider.validateAccessToken(token);
            String accessToken = token.get();

            if (Boolean.TRUE.equals(redisService.isBlacklisted(accessToken)))
                throw new BusinessException(ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);

            String email = jwtTokenProvider.getEmail(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);       // 웹소켓 연결이 유지되는 동안 서버 내 웹소켓 세션 저장소에 보관
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination(); // 예: /sub/chat/123
            if (destination != null && destination.startsWith("/sub/chat/")) {
                Long roomId = ParseUtil.parseLong(      // TODO: parseLong 메서드로 파싱 메서드 전부 변경
                        destination.replace("/sub/chat/", ""),
                        new BusinessException(ErrorCode.ROOM_NOT_FOUND)
                );

                CustomUserDetails userDetails = getCustomUserDetails(accessor);
                Long currentPos = redisService.getUserCurrentRoomId(userDetails.user().getId());

                if (currentPos == null || !currentPos.equals(roomId)) {
                    throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
                }
            }
        }

        if(StompCommand.SEND.equals(command)) {
            Optional<String> roomIdStr = Optional.ofNullable(accessor.getFirstNativeHeader("roomId"));
            Long roomId = ParseUtil.parseLong(roomIdStr, new BusinessException(ErrorCode.ROOM_NOT_FOUND));
            Room room = roomRepository.findById(roomId).orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

            CustomUserDetails userDetails = getCustomUserDetails(accessor);

            Long userId = userDetails.user().getId();
            User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            if(!user.getRoom().equals(room)) {
                throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            sessionAttributes.put("roomId", roomId);
        }

        return message;
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