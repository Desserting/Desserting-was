package com.desserting.backend.domain.user.service;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.dto.UpdateLocationRequest;
import com.desserting.backend.domain.user.dto.UpdateProfileRequest;
import com.desserting.backend.domain.user.dto.UserProfileResponse;
import com.desserting.backend.domain.user.dto.ProfileVisitorListResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserLikeRepository userLikeRepository;

    @Mock
    private ProfileVisitRepository profileVisitRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 프로필 정상 조회 시 취향 태그와 잔여 하트수가 정확히 반환된다")
    void getMyProfile_Success() {
        // given
        User user = User.builder()
                .email("me@desserting.com")
                .password("encoded")
                .nickname("바닐라빈라떼")
                .gender("MALE")
                .age(26)
                .region("대구 수성구")
                .bio("커피 좋아합니다")
                .dessertAvatarUrl(DessertType.COFFEE.getDefaultAvatarUrl())
                .heartCount(5)
                .isPremium(true)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.getDessertTags().add(UserDessertTag.of(user, DessertType.COFFEE));

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        // when
        UserProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("바닐라빈라떼");
        assertThat(response.getHeartCount()).isEqualTo(5);
        assertThat(response.getIsPremium()).isTrue();
        assertThat(response.getDessertTags()).containsExactly(DessertType.COFFEE);
    }

    @Test
    @DisplayName("존재하지 않는 회원 프로필 조회 시 USER_NOT_FOUND 예외 발생")
    void getMyProfile_NotFound_ThrowsException() {
        // given
        given(userRepository.findByIdWithTags(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("프로필 수정 시 닉네임, 소개글, 활동지역, 아바타, 선호태그가 정상 변경된다")
    void updateMyProfile_Success() {
        // given
        User user = User.builder()
                .nickname("옛날닉네임")
                .bio("옛날소개")
                .region("대구 중구")
                .dessertAvatarUrl(DessertType.CAKE.getDefaultAvatarUrl())
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        String newAvatarUrl = DessertType.BAKERY.getDefaultAvatarUrl();
        UpdateProfileRequest request = new UpdateProfileRequest(
                "새닉네임",
                "새소개글",
                "대구 북구",
                newAvatarUrl,
                List.of(DessertType.BAKERY, DessertType.CAKE)
        );

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        // when
        UserProfileResponse response = userService.updateMyProfile(1L, request);

        // then
        assertThat(response.getNickname()).isEqualTo("새닉네임");
        assertThat(response.getBio()).isEqualTo("새소개글");
        assertThat(response.getRegion()).isEqualTo("대구 북구");
        assertThat(response.getDessertAvatarUrl()).isEqualTo(newAvatarUrl);
        assertThat(response.getDessertTags()).containsExactlyInAnyOrder(DessertType.BAKERY, DessertType.CAKE);
    }

    @Test
    @DisplayName("프로필 수정 시 중복된 닉네임으로 변경을 시도하면 DUPLICATE_NICKNAME 예외 발생")
    void updateMyProfile_DuplicateNickname_ThrowsException() {
        // given
        User user = User.builder().nickname("기존닉네임").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "이미존재하는닉네임", null, null, null, null
        );

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("이미존재하는닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateMyProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("선택 가능한 아바타 목록에 없는 URL로 변경을 시도하면 INVALID_AVATAR_URL 예외 발생")
    void updateMyProfile_InvalidAvatarUrl_ThrowsException() {
        // given
        User user = User.builder().nickname("기존닉네임").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null, "https://desserting.com/avatars/unknown-random.png", null
        );

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.updateMyProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AVATAR_URL);
    }

    @Test
    @DisplayName("위치 갱신 시 위도/경도/지역명이 올바르게 업데이트된다")
    void updateLocation_Success() {
        // given
        User user = User.builder()
                .region("대구 중구")
                .latitude(BigDecimal.valueOf(35.868))
                .longitude(BigDecimal.valueOf(128.593))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UpdateLocationRequest request = new UpdateLocationRequest(
                BigDecimal.valueOf(35.871),
                BigDecimal.valueOf(128.601),
                "대구 수성구"
        );

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        // when
        UserProfileResponse response = userService.updateLocation(1L, request);

        // then
        assertThat(response.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(35.871));
        assertThat(response.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(128.601));
        assertThat(response.getRegion()).isEqualTo("대구 수성구");
    }

    @Test
    @DisplayName("타인 프로필 조회 시 민감정보(이메일, 잔여 하트수 등)는 제외되고 공개 프로필과 좋아요 여부가 반환된다")
    void getOtherUserProfile_Success() {
        // given
        User targetUser = User.builder()
                .email("secret@desserting.com")
                .password("encoded")
                .nickname("초코크로플")
                .gender("FEMALE")
                .age(24)
                .region("대구 동구")
                .bio("디저트 투어 좋아해요")
                .dessertAvatarUrl(DessertType.BAKERY.getDefaultAvatarUrl())
                .heartCount(10)
                .isPremium(true)
                .build();
        ReflectionTestUtils.setField(targetUser, "id", 99L);
        targetUser.getDessertTags().add(UserDessertTag.of(targetUser, DessertType.BAKERY));

        User viewerUser = User.builder().nickname("보는사람").build();
        ReflectionTestUtils.setField(viewerUser, "id", 1L);

        given(userRepository.findByIdWithTags(99L)).willReturn(Optional.of(targetUser));
        given(userRepository.findById(1L)).willReturn(Optional.of(viewerUser));
        given(profileVisitRepository.findByVisitorIdAndVisitedUserId(1L, 99L)).willReturn(Optional.empty());
        given(userLikeRepository.existsBySenderIdAndReceiverId(1L, 99L)).willReturn(true);

        // when
        OtherUserProfileResponse response = userService.getOtherUserProfile(1L, 99L);

        // then
        assertThat(response.getUserId()).isEqualTo(99L);
        assertThat(response.getNickname()).isEqualTo("초코크로플");
        assertThat(response.getIsPremium()).isTrue();
        assertThat(response.getLiked()).isTrue();
        assertThat(response.getDessertTags()).containsExactly(DessertType.BAKERY);
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 시 자신과 차단/피차단 사용자는 제외되고 좋아요 여부가 매핑된다")
    void getAllOtherUsers_ExcludesBlockedUsers() {
        // given
        User me = User.builder().nickname("나").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(me, "id", 1L);

        User normalUser = User.builder().nickname("일반유저").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(normalUser, "id", 2L);

        User blockedUser = User.builder().nickname("내가차단한유저").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(blockedUser, "id", 3L);

        User blockerUser = User.builder().nickname("나를차단한유저").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(blockerUser, "id", 4L);

        given(userRepository.findAllNormalUsersWithTags()).willReturn(List.of(me, normalUser, blockedUser, blockerUser));
        given(userBlockRepository.findBlockedUserIdsByUserId(1L)).willReturn(List.of(3L));
        given(userBlockRepository.findBlockerUserIdsByUserId(1L)).willReturn(List.of(4L));
        given(userLikeRepository.findLikedReceiverIdsBySenderId(1L)).willReturn(Set.of(2L));

        // when
        List<OtherUserProfileResponse> result = userService.getAllOtherUsers(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(0).getNickname()).isEqualTo("일반유저");
        assertThat(result.get(0).getLiked()).isTrue();
    }

    @Test
    @DisplayName("프리미엄 회원은 내 프로필 방문자 목록과 총 방문자 수를 조회할 수 있다")
    void getProfileVisitors_PremiumSuccess() {
        // given
        User premiumUser = User.builder().email("premium@desserting.com").nickname("프리미엄유저").isPremium(true).build();
        User visitor = User.builder().email("visitor@desserting.com").nickname("방문자1").build();
        ReflectionTestUtils.setField(visitor, "id", 10L);

        ProfileVisit visit = ProfileVisit.of(premiumUser, visitor);

        given(userRepository.findById(1L)).willReturn(Optional.of(premiumUser));
        given(profileVisitRepository.countByVisitedUserId(1L)).willReturn(1L);
        given(profileVisitRepository.findRecentVisitors(1L)).willReturn(List.of(visit));

        // when
        ProfileVisitorListResponse response = userService.getProfileVisitors(1L);

        // then
        assertThat(response.totalVisitorCount()).isEqualTo(1L);
        assertThat(response.visitors()).hasSize(1);
        assertThat(response.visitors().get(0).nickname()).isEqualTo("방문자1");
    }

    @Test
    @DisplayName("일반 회원이 프로필 방문자 조회를 시도하면 ACCESS_DENIED 예외가 발생한다")
    void getProfileVisitors_NormalUserThrowsAccessDenied() {
        // given
        User normalUser = User.builder().email("normal@desserting.com").nickname("일반유저").isPremium(false).build();
        given(userRepository.findById(2L)).willReturn(Optional.of(normalUser));

        // when & then
        assertThatThrownBy(() -> userService.getProfileVisitors(2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
