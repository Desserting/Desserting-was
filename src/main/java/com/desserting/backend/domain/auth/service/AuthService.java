package com.desserting.backend.domain.auth.service;

import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.desserting.backend.domain.auth.dto.RefreshTokenRequest;
import com.desserting.backend.domain.auth.dto.SignUpRequest;
import com.desserting.backend.domain.auth.dto.TokenResponse;
import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HeartHistoryRepository heartHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // PRF-02: 첫 번째 선택한 디저트 종류를 기반으로 기본 대표 아바타 이미지 자동 할당
        // 아바타를 직접 고른 경우 기본 제공 8종 중 하나인지 검증하고, 없으면 첫 번째 취향 기준으로 자동 지정
        String defaultAvatarUrl = request.getDessertTypes().get(0).getDefaultAvatarUrl();
        String requestedAvatarUrl = request.getDessertAvatarUrl();
        if (requestedAvatarUrl != null && !requestedAvatarUrl.isBlank()) {
            if (!DessertType.isSelectableAvatarUrl(requestedAvatarUrl)) {
                throw new CustomException(ErrorCode.INVALID_AVATAR_URL);
            }
            defaultAvatarUrl = requestedAvatarUrl;
        }

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

        // HRT-01: 신규 가입 웰컴 무료 하트 5개 지급 이력 저장
        heartHistoryRepository.save(HeartHistory.of(
                savedUser,
                5,
                5,
                HeartHistoryType.WELCOME_BONUS,
                "신규 회원가입 웰컴 무료 하트 5개 지급"
        ));

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

        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN, "유효한 Refresh Token이 아닙니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Redis 기반 Refresh Token 일치 여부 확인 (탈취/중복 갱신 방어)
        if (redisTemplate != null) {
            try {
                String key = "auth:refresh:" + userId;
                String savedToken = redisTemplate.opsForValue().get(key);
                if (savedToken != null && !savedToken.equals(token)) {
                    log.warn("Refresh Token 불일치 감지 (탈취 의심 또는 이미 갱신됨): userId={}", userId);
                    throw new CustomException(ErrorCode.INVALID_JWT_TOKEN, "이미 사용되었거나 무효화된 Refresh Token입니다.");
                }
            } catch (CustomException ce) {
                throw ce;
            } catch (Exception e) {
                log.warn("Redis Refresh Token 검증 실패 (fallback 유지): {}", e.getMessage());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("JWT 토큰 갱신: userId={}", user.getId());
        return createTokenResponse(user);
    }

    public void logout(Long userId) {
        if (userId != null && redisTemplate != null) {
            try {
                redisTemplate.delete("auth:refresh:" + userId);
                log.info("로그아웃 완료 (Refresh Token 무효화): userId={}", userId);
            } catch (Exception e) {
                log.warn("Redis Refresh Token 삭제 실패: {}", e.getMessage());
            }
        }
    }

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessExpirationMs());
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        if (redisTemplate != null) {
            try {
                String key = "auth:refresh:" + userId;
                long ttl = jwtTokenProvider.getRefreshExpirationMs();
                redisTemplate.opsForValue().set(key, refreshToken, ttl, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("Redis Refresh Token 저장 실패 (fallback 유지): {}", e.getMessage());
            }
        }
    }
}
