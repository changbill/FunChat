package com.funchat.demo.auth.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.TokenType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("type", TokenType.ACCESS.getValue())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("type", TokenType.REFRESH.getValue())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Long getExpiration(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public void validateAccessToken(String accessToken) {
        try {
            Claims payload = Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getPayload();
            String type = payload.get("type", String.class);
            if (!TokenType.ACCESS.getValue().equals(type)) {        // Access Token을 탈취해서 Refresh Token인 척 재발급 API 공격 방지
                throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
            }
        } catch (SecurityException | MalformedJwtException e) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_ACCESS_TOKEN);
        } catch (IllegalArgumentException e) {      // token이 null인 경우 여기에 해당
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_NOT_FOUND);
        }
    }

    public void validateRefreshToken(String refreshToken) {
        try {
            Claims payload = Jwts.parser().setSigningKey(key).build().parseClaimsJws(refreshToken).getPayload();
            String type = payload.get("type", String.class);
            if (!TokenType.REFRESH.getValue().equals(type)) {       // Access Token을 탈취해서 Refresh Token인 척 재발급 API 공격 방지
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        } catch (SecurityException | MalformedJwtException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_REFRESH_TOKEN);
        } catch (IllegalArgumentException e) {      // token이 null인 경우 여기에 해당
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
    }
}