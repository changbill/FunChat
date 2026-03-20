package com.funchat.demo.global.filter;

import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.auth.util.AuthUtil;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> optionalToken = AuthUtil.resolveToken(request.getHeader("Authorization"));
        if (optionalToken.isPresent()) {
            jwtTokenProvider.validateAccessToken(optionalToken);

            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + optionalToken)))
                throw new BusinessException(ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);

            String accessToken = optionalToken.get();
            String email = jwtTokenProvider.getEmail(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 토큰이 없더라도 다음 필터로 넘김. 인증이 필요없는 API일 수 있기 때문. 인증이 필요한 API의 경우 Security가 나중에 거절할 것임.
        filterChain.doFilter(request, response);
    }
}