package com.desserting.backend.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // 256 bits secret
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKeyString", "ZGVzc2VydGluZy1zZWNyZXQta2V5LTIwMjYtZGFuZGl0aG9uLWJhY2tlbmQtand0LXRva2VuLXNlY3VyZQ==");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 1209600000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Access Token 생성 및 subject(userId), claim(email) 정상 추출 검증")
    void generateAndParseAccessToken() {
        // given
        Long userId = 42L;
        String email = "test@desserting.com";
        String role = "ROLE_USER";

        // when
        String token = jwtTokenProvider.generateAccessToken(userId, email, role);

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@desserting.com");
    }

    @Test
    @DisplayName("Refresh Token 생성 및 subject(userId) 정상 추출 검증")
    void generateAndParseRefreshToken() {
        // given
        Long userId = 42L;

        // when
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // then
        assertThat(refreshToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(42L);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token의 타입(typ)이 정확히 구분되어 상호 오용이 방지된다")
    void tokenTypeDistinction() {
        // given
        String accessToken = jwtTokenProvider.generateAccessToken(1L, "user@desserting.com", "ROLE_USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        // when & then
        assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();

        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
    }
}
