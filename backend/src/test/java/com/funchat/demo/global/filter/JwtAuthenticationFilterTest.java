package com.funchat.demo.global.filter;

import com.funchat.demo.auth.domain.dto.CustomUserDetails;
import com.funchat.demo.auth.service.CustomUserDetailsService;
import com.funchat.demo.auth.service.JwtTokenProvider;
import com.funchat.demo.chat.service.redis.RedisService;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private RedisService redisService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization access token이 유효하면 SecurityContext에 인증 정보를 저장한다")
    void doFilterInternal_AuthenticatesValidAccessToken() throws ServletException, IOException {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, redisService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        User user = User.createForTest(1L, "user@test.com", "tester", "");
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(redisService.isBlacklisted("access-token")).thenReturn(false);
        when(jwtTokenProvider.getEmail("access-token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateAccessToken(Optional.of("access-token"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 통과한다")
    void doFilterInternal_PassesThroughWithoutToken() throws ServletException, IOException {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, redisService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).validateAccessToken(Optional.empty());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("블랙리스트된 access token은 인증을 거부한다")
    void doFilterInternal_RejectsBlacklistedAccessToken() {
        // given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, redisService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisService.isBlacklisted("access-token")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LOGOUT_ACCESS_TOKEN);

        verify(jwtTokenProvider).validateAccessToken(Optional.of("access-token"));
        verify(userDetailsService, never()).loadUserByUsername("user@test.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
