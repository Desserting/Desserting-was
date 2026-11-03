package com.desserting.backend.domain.user.service;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.dto.UpdateLocationRequest;
import com.desserting.backend.domain.user.dto.UpdateProfileRequest;
import com.desserting.backend.domain.user.dto.UserProfileResponse;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdWithTags(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdWithTags(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        user.updateProfile(
                request.getNickname(),
                request.getBio(),
                request.getRegion(),
                request.getDessertAvatarUrl()
        );

        if (request.getDessertTypes() != null && !request.getDessertTypes().isEmpty()) {
            user.getDessertTags().clear();
            for (DessertType dessertType : request.getDessertTypes()) {
                user.getDessertTags().add(UserDessertTag.of(user, dessertType));
            }
        }

        log.info("사용자 프로필 수정 완료: userId={}, nickname={}", user.getId(), user.getNickname());
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateLocation(Long userId, UpdateLocationRequest request) {
        User user = userRepository.findByIdWithTags(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateLocation(request.getLatitude(), request.getLongitude(), request.getRegion());
        log.info("사용자 위치 갱신: userId={}, lat={}, lng={}, region={}", userId, request.getLatitude(), request.getLongitude(), request.getRegion());

        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public OtherUserProfileResponse getOtherUserProfile(Long targetUserId) {
        User targetUser = userRepository.findByIdWithTags(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "상대방 사용자를 찾을 수 없습니다: " + targetUserId));
        return OtherUserProfileResponse.from(targetUser);
    }

    @Transactional(readOnly = true)
    public List<OtherUserProfileResponse> getAllOtherUsers(Long currentUserId) {
        List<User> users = userRepository.findAllWithTags();
        if (currentUserId == null) {
            return users.stream()
                    .map(OtherUserProfileResponse::from)
                    .toList();
        }

        Set<Long> excludedUserIds = new HashSet<>();
        excludedUserIds.add(currentUserId);
        excludedUserIds.addAll(userBlockRepository.findBlockedUserIdsByUserId(currentUserId));
        excludedUserIds.addAll(userBlockRepository.findBlockerUserIdsByUserId(currentUserId));

        return users.stream()
                .filter(u -> !excludedUserIds.contains(u.getId()))
                .map(OtherUserProfileResponse::from)
                .toList();
    }
}
