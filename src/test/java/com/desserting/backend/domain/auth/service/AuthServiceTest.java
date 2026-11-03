package com.desserting.backend.domain.auth.service;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.desserting.backend.domain.auth.dto.RefreshTokenRequest;
import com.desserting.backend.domain.auth.dto.SignUpRequest;
import com.desserting.backend.domain.auth.dto.TokenResponse;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("정상 회원가입 시 5개 무료 하트와 디저트 취향 아바타가 자동 부여되고 토큰이 반환된다")
    void signUp_Success() {
        // given
        SignUpRequest request = SignUpRequest.builder()
                .email("newuser@desserting.com")
                .password("password123!")
                .nickname("딸기케이크")
                .gender("FEMALE")
                .age(24)
                .region("대구 남구")
                .dessertTypes(List.of(DessertType.CAKE, DessertType.COFFEE))
                .agreeToTerms(true)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        given(jwtTokenProvider.generateAccessToken(1L, "newuser@desserting.com", "ROLE_USER"))
                .willReturn("mockAccessToken");
        given(jwtTokenProvider.generateRefreshToken(1L))
                .willReturn("mockRefreshToken");
        given(jwtTokenProvider.getAccessExpirationMs()).willReturn(3600000L);

        // when
        TokenResponse response = authService.signUp(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mockAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("mockRefreshToken");
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 DUPLICATE_EMAIL 예외 발생")
    void signUp_DuplicateEmail() {
        // given
        SignUpRequest request = SignUpRequest.builder()
                .email("exist@desserting.com")
                .password("password123!")
                .nickname("새닉네임")
                .gender("MALE")
                .age(25)
                .region("대구 중구")
                .dessertTypes(List.of(DessertType.BAKERY))
                .agreeToTerms(true)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("이미 존재하는 닉네임으로 회원가입 시 DUPLICATE_NICKNAME 예외 발생")
    void signUp_DuplicateNickname() {
        // given
        SignUpRequest request = SignUpRequest.builder()
                .email("test2@desserting.com")
                .password("password123!")
                .nickname("중복닉네임")
                .gender("MALE")
                .age(25)
                .region("대구 중구")
                .dessertTypes(List.of(DessertType.BAKERY))
                .agreeToTerms(true)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인 성공")
    void login_Success() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email("login@desserting.com")
                .password("password123!")
                .build();

        User user = User.builder()
                .email(request.getEmail())
                .password("encodedPassword")
                .nickname("초코크로플")
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123!", "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(2L, "login@desserting.com", "ROLE_USER")).willReturn("accessToken");
        given(jwtTokenProvider.generateRefreshToken(2L)).willReturn("refreshToken");
        given(jwtTokenProvider.getAccessExpirationMs()).willReturn(3600000L);

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 UNAUTHORIZED 예외 발생")
    void login_WrongPassword() {
        // given
        LoginRequest request = LoginRequest.builder()
                .email("login@desserting.com")
                .password("wrong_password")
                .build();

        User user = User.builder()
                .email(request.getEmail())
                .password("encodedPassword")
                .nickname("초코크로플")
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong_password", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 새 토큰 쌍 재발급 성공")
    void refreshToken_Success() {
        // given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("validRefreshToken")
                .build();

        User user = User.builder()
                .email("refresh@desserting.com")
                .nickname("마카롱")
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);

        given(jwtTokenProvider.validateToken("validRefreshToken")).willReturn(true);
        given(jwtTokenProvider.isRefreshToken("validRefreshToken")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("validRefreshToken")).willReturn(3L);
        given(userRepository.findById(3L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(3L, "refresh@desserting.com", "ROLE_USER")).willReturn("newAccessToken");
        given(jwtTokenProvider.generateRefreshToken(3L)).willReturn("newRefreshToken");
        given(jwtTokenProvider.getAccessExpirationMs()).willReturn(3600000L);

        // when
        TokenResponse response = authService.refreshToken(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("Refresh Token 자리에 Access Token 전달 시 INVALID_JWT_TOKEN 예외 발생")
    void refreshToken_ThrowsException_WhenNotRefreshToken() {
        // given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("accessTokenPassedAsRefreshToken")
                .build();

        given(jwtTokenProvider.validateToken("accessTokenPassedAsRefreshToken")).willReturn(true);
        given(jwtTokenProvider.isRefreshToken("accessTokenPassedAsRefreshToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }
}
