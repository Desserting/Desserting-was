package com.desserting.backend.domain.matching.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.matching.dto.MatchCandidateResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse.AppliedFilter;
import com.desserting.backend.domain.matching.entity.MatchType;
import com.desserting.backend.domain.matching.repository.MatchCandidateRepository;
import com.desserting.backend.domain.matching.repository.MatchExposureRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchCandidateRepository matchCandidateRepository;
    @Mock
    private MatchExposureRepository matchExposureRepository;

    @InjectMocks
    private MatchingService matchingService;

    // 앞산 근처
    private static final double ME_LAT = 35.8290;
    private static final double ME_LNG = 128.5850;

    @Test
    @DisplayName("같은 취향 매칭은 겹치는 디저트가 있는 이성만 반환하고 노출 기록을 남긴다")
    void sameTaste_returnsOverlappingOppositeGender() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE, DessertType.COFFEE);
        User sameTasteMale = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User diffTasteMale = user(3L, "MALE", "대구 남구", null, null, DessertType.BAKERY);
        User sameTasteFemale = user(4L, "FEMALE", "대구 남구", null, null, DessertType.COFFEE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L))
                .willReturn(List.of(sameTasteMale, diffTasteMale, sameTasteFemale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getCandidate().getCommonDessertTags()).containsExactly(DessertType.CAKE);
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.ALL);
        verify(matchExposureRepository).markSeen(1L, 2L);
    }

    @Test
    @DisplayName("다른 취향 매칭은 디저트가 겹치지 않는 이성만 반환한다")
    void differentTaste_returnsNonOverlappingOppositeGender() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User diffTasteMale = user(2L, "MALE", "대구 남구", null, null, DessertType.BAKERY);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(diffTasteMale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.DIFFERENT_TASTE, null, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getCandidate().getCommonDessertTags()).isEmpty();
    }

    @Test
    @DisplayName("내 위치가 있고 거리 필터가 주어지면 반경 내 이성만 반환한다")
    void distanceFilter_returnsWithinRadius() {
        User me = user(1L, "MALE", "대구 남구", ME_LAT, ME_LNG, DessertType.CAKE);
        // 약 2km 거리
        User nearFemale = user(2L, "FEMALE", "대구 남구", 35.8450, 128.5850, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesInBox(eq(1L), any(), any(), any(), any()))
                .willReturn(List.of(nearFemale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 5.0, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.DISTANCE);
        assertThat(response.getCandidate().getApproxDistanceKm()).isNotNull();
    }

    @Test
    @DisplayName("내 위치 정보가 없을 때 거리 필터를 요청하면 내 활동 지역으로 자동 대체된다 (LOC-03)")
    void distanceFilter_fallsBackToMyRegion_whenNoLocation() {
        User me = user(1L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User regionalFemale = user(2L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "대구 남구")).willReturn(List.of(regionalFemale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 5.0, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
        assertThat(response.getGuide()).contains("내 활동 지역");
    }

    @Test
    @DisplayName("지역 필터가 주어지면 해당 지역 후보만 반환한다")
    void regionFilter_returnsMatchingRegion() {
        User me = user(1L, "MALE", "대구 중구", null, null, DessertType.CAKE);
        User dongseongroFemale = user(2L, "FEMALE", "대구 중구 동성로", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "대구 중구")).willReturn(List.of(dongseongroFemale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구 중구");

        assertThat(response.isFound()).isTrue();
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
    }

    @Test
    @DisplayName("지역 키워드가 '전체' 또는 '대구·경북'이면 지역 필터를 무시하고 전체 풀을 조회한다")
    void wholeAreaKeyword_ignoresRegionFilter() {
        User me = user(1L, "MALE", "대구 중구", null, null, DessertType.CAKE);
        User candidate = user(2L, "FEMALE", "포항시", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(candidate));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구·경북");

        assertThat(response.isFound()).isTrue();
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.ALL);
    }

    @Test
    @DisplayName("후보가 없을 때는 found=false 와 안내 문구를 반환한다")
    void noCandidates_returnsNotFound() {
        User me = user(1L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isFound()).isFalse();
        assertThat(response.getCandidate()).isNull();
        assertThat(response.getGuide()).isNotBlank();
        verify(matchExposureRepository, never()).markSeen(anyLong(), anyLong());
    }

    @Test
    @DisplayName("모든 후보를 다 본 경우 노출 기록을 비우고 다시 순환한다 (recycled=true)")
    void allSeen_clearsExposureAndRecycles() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User seenMale = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(seenMale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of(2L));

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.isRecycled()).isTrue();
        verify(matchExposureRepository).clear(1L);
        verify(matchExposureRepository).markSeen(1L, 2L);
    }

    @Test
    @DisplayName("차단 관계인 상대는 후보에서 제외한다 (SEC-01)")
    void excludesBlockedUsers() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User blocked = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User ok = user(3L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(blocked, ok));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of(2L));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.getCandidate().getUserId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("반경이 허용 범위를 벗어나면 INVALID_MATCH_FILTER")
    void invalidRadius_throws() {
        assertThatThrownBy(() -> matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 500.0, null))
                .isInstanceOf(ExploreException.class)
                .hasFieldOrPropertyWithValue("errorCode", ExploreErrorCode.INVALID_MATCH_FILTER);
    }

    @Test
    @DisplayName("프리미엄 회원이 아닌 경우 스킵한 상대 이력 조회 시 ACCESS_DENIED 예외 발생")
    void getSkippedMatchHistory_DeniedForNonPremium() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        ReflectionTestUtils.setField(me, "isPremium", false);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));

        assertThatThrownBy(() -> matchingService.getSkippedMatchHistory(1L, 20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("프리미엄 회원은 스킵한 상대 이력을 Fetch Join으로 일괄 조회하여 성공적으로 반환한다")
    void getSkippedMatchHistory_Success() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        ReflectionTestUtils.setField(me, "isPremium", true);

        User skippedCandidate = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchExposureRepository.findHistoryUserIds(1L, 20)).willReturn(List.of(2L));
        given(userRepository.findAllByIdInWithTags(List.of(2L))).willReturn(List.of(skippedCandidate));

        List<MatchCandidateResponse> history = matchingService.getSkippedMatchHistory(1L, 20);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getUserId()).isEqualTo(2L);
        assertThat(history.get(0).getCommonDessertTags()).containsExactly(DessertType.CAKE);
    }

    private static User user(Long id, String gender, String region, Double lat, Double lng, DessertType... tags) {
        User user = User.builder()
                .email("u" + id + "@desserting.com")
                .password("encoded")
                .nickname("user" + id)
                .gender(gender)
                .age(25)
                .region(region)
                .latitude(lat == null ? null : BigDecimal.valueOf(lat))
                .longitude(lng == null ? null : BigDecimal.valueOf(lng))
                .dessertTags(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        for (DessertType tag : tags) {
            user.getDessertTags().add(UserDessertTag.of(user, tag));
        }
        return user;
    }
}
