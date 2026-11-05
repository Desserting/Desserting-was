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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                .heartCount(4)
                .isPremium(false)
                .dessertAvatarUrl("https://desserting-web.vercel.app/avatars/coffee.png")
                .dessertTags(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        user.getDessertTags().add(UserDessertTag.of(user, DessertType.COFFEE));

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        // when
        UserProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("바닐라빈라떼");
        assertThat(response.getHeartCount()).isEqualTo(4);
        assertThat(response.getDessertTags()).containsExactly(DessertType.COFFEE);
    }

    @Test
    @DisplayName("프로필 수정 시 닉네임, 소개글, 디저트 취향 태그가 정상 갱신된다")
    void updateMyProfile_Success() {
        // given
        User user = User.builder()
                .email("me@desserting.com")
                .nickname("기존닉네임")
                .bio("기존 소개")
                .region("대구 남구")
                .dessertTags(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.getDessertTags().add(UserDessertTag.of(user, DessertType.CAKE));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("새로운닉네임")
                .bio("새로운 자기소개입니다")
                .region("대구 중구")
                .dessertTypes(List.of(DessertType.BAKERY, DessertType.TEA_NON_COFFEE))
                .build();

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새로운닉네임")).willReturn(false);

        // when
        UserProfileResponse response = userService.updateMyProfile(1L, request);

        // then
        assertThat(response.getNickname()).isEqualTo("새로운닉네임");
        assertThat(response.getBio()).isEqualTo("새로운 자기소개입니다");
        assertThat(response.getRegion()).isEqualTo("대구 중구");
        assertThat(response.getDessertTags()).containsExactly(DessertType.BAKERY, DessertType.TEA_NON_COFFEE);
    }

    @Test
    @DisplayName("프로필 수정 시 기본 제공 아바타 URL이면 대표 아바타가 변경된다")
    void updateMyProfile_SelectableAvatar() {
        User user = User.builder().email("me@desserting.com").nickname("닉네임")
                .dessertAvatarUrl(DessertType.CAKE.getDefaultAvatarUrl()).dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.updateMyProfile(1L, UpdateProfileRequest.builder()
                .dessertAvatarUrl(DessertType.TEA_NON_COFFEE.getDefaultAvatarUrl()).build());

        assertThat(response.getDessertAvatarUrl()).isEqualTo("https://desserting-web.vercel.app/avatars/tea.png");
    }

    @Test
    @DisplayName("프로필 수정 시 기본 제공 목록에 없는 아바타 URL이면 INVALID_AVATAR_URL 예외 발생")
    void updateMyProfile_InvalidAvatar() {
        User user = User.builder().email("me@desserting.com").nickname("닉네임").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateMyProfile(1L, UpdateProfileRequest.builder()
                .dessertAvatarUrl("https://evil.example.com/a.png").build()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AVATAR_URL);
    }

    @Test
    @DisplayName("프로필 수정 시 이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외 발생")
    void updateMyProfile_DuplicateNickname() {
        // given
        User user = User.builder()
                .email("me@desserting.com")
                .nickname("기존닉네임")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("이미있는닉네임")
                .build();

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("이미있는닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateMyProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("위치 정보(위경도 및 수동 지역) 정상 갱신 검증")
    void updateLocation_Success() {
        // given
        User user = User.builder()
                .email("me@desserting.com")
                .region("대구 남구")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .latitude(new BigDecimal("35.8398120"))
                .longitude(new BigDecimal("128.5876320"))
                .region("앞산 카페거리")
                .build();

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(user));

        // when
        UserProfileResponse response = userService.updateLocation(1L, request);

        // then
        assertThat(response.getLatitude()).isEqualTo(new BigDecimal("35.8398120"));
        assertThat(response.getLongitude()).isEqualTo(new BigDecimal("128.5876320"));
        assertThat(response.getRegion()).isEqualTo("앞산 카페거리");
    }

    @Test
    @DisplayName("타인 프로필 열람 시 민감 정보(이메일, 잔여 하트 등)는 노출되지 않고 공개 정보만 반환된다")
    void getOtherUserProfile_Success() {
        // given
        User targetUser = User.builder()
                .email("private@desserting.com")
                .password("secret")
                .nickname("초코크로플")
                .gender("FEMALE")
                .age(23)
                .region("대구 수성구")
                .heartCount(10)
                .isPremium(true)
                .dessertAvatarUrl("https://desserting-web.vercel.app/avatars/bakery.png")
                .dessertTags(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(targetUser, "id", 99L);
        targetUser.getDessertTags().add(UserDessertTag.of(targetUser, DessertType.BAKERY));

        given(userRepository.findByIdWithTags(99L)).willReturn(Optional.of(targetUser));

        // when
        OtherUserProfileResponse response = userService.getOtherUserProfile(99L);

        // then
        assertThat(response.getUserId()).isEqualTo(99L);
        assertThat(response.getNickname()).isEqualTo("초코크로플");
        assertThat(response.getIsPremium()).isTrue();
        assertThat(response.getDessertTags()).containsExactly(DessertType.BAKERY);
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 시 자신과 차단/피차단 사용자는 제외된다")
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

        given(userRepository.findAllWithTags()).willReturn(List.of(me, normalUser, blockedUser, blockerUser));
        given(userBlockRepository.findBlockedUserIdsByUserId(1L)).willReturn(List.of(3L));
        given(userBlockRepository.findBlockerUserIdsByUserId(1L)).willReturn(List.of(4L));

        // when
        List<OtherUserProfileResponse> result = userService.getAllOtherUsers(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(0).getNickname()).isEqualTo("일반유저");
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
