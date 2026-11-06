package com.desserting.backend.domain.user.service;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.dto.ProfileVisitorListResponse;
import com.desserting.backend.domain.user.dto.ProfileVisitorResponse;
import com.desserting.backend.domain.user.dto.UpdateLocationRequest;
import com.desserting.backend.domain.user.dto.UpdateProfileRequest;
import com.desserting.backend.domain.user.dto.UserProfileResponse;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.ProfileVisit;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.ProfileVisitRepository;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserLikeRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserLikeRepository userLikeRepository;
    private final ProfileVisitRepository profileVisitRepository;

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

        String avatarUrl = request.getDessertAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isBlank() && !DessertType.isSelectableAvatarUrl(avatarUrl)) {
            throw new CustomException(ErrorCode.INVALID_AVATAR_URL);
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

    @Transactional
    public OtherUserProfileResponse getOtherUserProfile(Long viewerUserId, Long targetUserId) {
        User targetUser = userRepository.findByIdWithTags(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "상대방 사용자를 찾을 수 없습니다: " + targetUserId));

        if (viewerUserId != null && !viewerUserId.equals(targetUserId)) {
            userRepository.findById(viewerUserId).ifPresent(viewer -> {
                profileVisitRepository.findByVisitorIdAndVisitedUserId(viewerUserId, targetUserId)
                        .ifPresentOrElse(
                                ProfileVisit::updateVisitedAt,
                                () -> profileVisitRepository.save(ProfileVisit.of(targetUser, viewer))
                        );
            });
        }

        Boolean liked = viewerUserId != null
                ? userLikeRepository.existsBySenderIdAndReceiverId(viewerUserId, targetUserId)
                : null;

        return OtherUserProfileResponse.from(targetUser, liked);
    }

    @Transactional(readOnly = true)
    public OtherUserProfileResponse getOtherUserProfile(Long targetUserId) {
        return getOtherUserProfile(null, targetUserId);
    }

    @Transactional(readOnly = true)
    public ProfileVisitorListResponse getProfileVisitors(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsPremium())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "프리미엄 회원만 프로필 방문자 목록을 확인할 수 있습니다.");
        }

        long totalCount = profileVisitRepository.countByVisitedUserId(userId);
        List<ProfileVisitorResponse> visitors = profileVisitRepository.findRecentVisitors(userId).stream()
                .map(ProfileVisitorResponse::from)
                .toList();

        return ProfileVisitorListResponse.of(totalCount, visitors);
    }

    @Transactional(readOnly = true)
    public List<OtherUserProfileResponse> getAllOtherUsers(Long currentUserId) {
        List<User> users = userRepository.findAllUsersWithTags();
        if (currentUserId == null) {
            return users.stream()
                    .map(u -> OtherUserProfileResponse.from(u, null))
                    .toList();
        }

        Set<Long> excludedUserIds = new HashSet<>();
        excludedUserIds.add(currentUserId);
        excludedUserIds.addAll(userBlockRepository.findBlockedUserIdsByUserId(currentUserId));
        excludedUserIds.addAll(userBlockRepository.findBlockerUserIdsByUserId(currentUserId));

        Set<Long> likedUserIds = userLikeRepository.findLikedReceiverIdsBySenderId(currentUserId);

        return users.stream()
                .filter(u -> !excludedUserIds.contains(u.getId()))
                .map(u -> OtherUserProfileResponse.from(u, likedUserIds.contains(u.getId())))
                .toList();
    }
}
