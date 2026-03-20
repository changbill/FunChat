package com.funchat.demo.auth;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.RoomUserRepository;
import com.funchat.demo.util.ParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CustomUserDetailsService userDetailsService;
    private final RoomUserRepository roomUserRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT == command) {
            Optional<String> token = extractToken(accessor);
            jwtTokenProvider.validateAccessToken(token);
            String accessToken = token.get();

            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + accessToken)))
                throw new BusinessException(ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);

            String email = jwtTokenProvider.getEmail(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);       // 웹소켓 연결이 유지되는 동안 서버 내 웹소켓 세션 저장소에 보관
        }

        if (StompCommand.SUBSCRIBE == command) {
            String destination = accessor.getDestination(); // 예: /sub/chat/123
            if (destination != null && destination.startsWith("/sub/chat/")) {
                Long roomId = ParseUtil.parseLong(      // TODO: parseLong 메서드로 파싱 메서드 전부 변경
                        destination.replace("/sub/chat/", ""),
                        new BusinessException(ErrorCode.ROOM_NOT_FOUND)
                );

                Authentication auth = (Authentication) accessor.getUser();
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                Object currentPos = redisTemplate.opsForValue().get("user:pos:" + userDetails.user().getId());

                if (currentPos == null || !currentPos.toString().equals(roomId.toString())) {
                    throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
                }
            }
        }

        if(StompCommand.SEND == command) {
            Optional<String> roomIdStr = Optional.ofNullable(accessor.getFirstNativeHeader("roomId"));
            Long roomId = Long.parseLong(roomIdStr.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND)));

            Optional<Authentication> optionalAuthentication = Optional.ofNullable((Authentication) accessor.getUser());
            Authentication authentication = optionalAuthentication.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.user().getId();

            if(!roomUserRepository.existsByRoomIdAndUserId(roomId, userId)) {
                throw new BusinessException(ErrorCode.ROOM_USER_NOT_PARTICIPANT);
            }

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            sessionAttributes.put("roomId", roomId);
        }

        return message;
    }



    private Optional<String> extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        return AuthUtil.resolveToken(bearerToken);
    }
}