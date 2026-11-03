package com.desserting.backend.domain.auth.service;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.desserting.backend.domain.auth.dto.RefreshTokenRequest;
import com.desserting.backend.domain.auth.dto.SignUpRequest;
import com.desserting.backend.domain.auth.dto.TokenResponse;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // PRF-02: 첫 번째 선택한 디저트 종류를 기반으로 기본 대표 아바타 이미지 자동 할당
        DessertType primaryDessert = request.getDessertTypes().get(0);
        String defaultAvatarUrl = primaryDessert.getDefaultAvatarUrl();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .gender(request.getGender())
                .age(request.getAge())
                .region(request.getRegion())
                .heartCount(5) // 기본 무료 하트 5개 지급 (HRT-01)
                .isPremium(false)
                .dessertAvatarUrl(defaultAvatarUrl)
                .role(UserRole.ROLE_USER)
                .build();

        // PRF-01: 선호 디저트 태그 저장
        for (DessertType type : request.getDessertTypes()) {
            user.getDessertTags().add(UserDessertTag.of(user, type));
        }

        User savedUser = userRepository.save(user);
        log.info("신규 회원가입 완료: userId={}, email={}, nickname={}", savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());

        return createTokenResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        log.info("로그인 성공: userId={}, email={}", user.getId(), user.getEmail());
        return createTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        jwtTokenProvider.validateToken(token);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("JWT 토큰 갱신: userId={}", user.getId());
        return createTokenResponse(user);
    }

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessExpirationMs());
    }
}
