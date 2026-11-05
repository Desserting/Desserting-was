package com.desserting.backend.domain.matching.service;

import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse.AppliedFilter;
import com.desserting.backend.domain.matching.entity.MatchType;
import com.desserting.backend.domain.matching.repository.MatchCandidateRepository;
import com.desserting.backend.domain.matching.repository.MatchExposureRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.UserRepository;
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
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(sameTasteMale, diffTasteMale, sameTasteFemale));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isFound()).isTrue();
        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.ALL);
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getCandidate().getCommonDessertTags()).containsExactly(DessertType.CAKE);
        assertThat(response.getCandidate().getApproxDistanceKm()).isNull();
        verify(matchExposureRepository).markSeen(1L, 2L);
    }

    @Test
    @DisplayName("다른 취향 매칭은 겹치는 디저트가 없는 상대만 반환한다")
    void differentTaste_returnsNonOverlapping() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User same = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User diff = user(3L, "MALE", "대구 남구", null, null, DessertType.BAKERY);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(same, diff));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.DIFFERENT_TASTE, null, null);

        assertThat(response.getCandidate().getUserId()).isEqualTo(3L);
        assertThat(response.getCandidate().getCommonDessertTags()).isEmpty();
    }

    @Test
    @DisplayName("반경 필터는 박스 조회 후 실제 거리로 다시 거르고, 거리는 올림 정수 km로만 노출한다")
    void distanceFilter_excludesOutsideRadius() {
        User me = user(1L, "FEMALE", "대구 남구", ME_LAT, ME_LNG, DessertType.CAKE);
        User near = user(2L, "MALE", "대구 남구", 35.8301, 128.5866, DessertType.CAKE);   // 약 0.2km
        User boxCorner = user(3L, "MALE", "대구 남구", ME_LAT + 0.0170, ME_LNG + 0.0210, DessertType.CAKE); // 박스 안, 반경 밖

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesInBox(eq(1L), any(), any(), any(), any()))
                .willReturn(List.of(near, boxCorner));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 2.0, null);

        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.DISTANCE);
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(response.getCandidate().getApproxDistanceKm()).isEqualTo(1);
    }

    @Test
    @DisplayName("위치정보가 없는데 반경을 요청하면 내 활동 지역으로 대체한다 (LOC-03)")
    void noLocation_fallsBackToMyRegion() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User candidate = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "대구 남구")).willReturn(List.of(candidate));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 10.0, null);

        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
        assertThat(response.getGuide()).contains("활동 지역");
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("지역을 쉼표로 여러 개 보내면 그중 하나에 속한 상대를 찾는다 (생활권 단위)")
    void multipleRegions_matchAny() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User jung = user(2L, "MALE", "대구 중구", null, null, DessertType.CAKE);
        User suseong = user(3L, "MALE", "대구 수성구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(jung, suseong));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구 중구, 대구 남구");

        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("반경과 지역을 함께 보내면 두 조건을 모두 만족하는 상대만 찾는다 (AND)")
    void distanceAndRegion_appliesBoth() {
        User me = user(1L, "FEMALE", "대구 남구", ME_LAT, ME_LNG, DessertType.CAKE);
        User nearNamgu = user(2L, "MALE", "대구 남구", 35.8301, 128.5866, DessertType.CAKE);
        User nearDalseo = user(3L, "MALE", "대구 달서구", 35.8305, 128.5860, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesInBox(eq(1L), any(), any(), any(), any()))
                .willReturn(List.of(nearNamgu, nearDalseo));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 5.0, "대구 남구");

        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.DISTANCE_AND_REGION);
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("'대구·경북' 지역은 전체 검색으로 처리한다")
    void wholeAreaRegion_meansAll() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구·경북");

        assertThat(response.getAppliedFilter()).isEqualTo(AppliedFilter.ALL);
    }

    @Test
    @DisplayName("이미 본 상대는 제외하고 다음 상대를 보여준다 (MAT-03)")
    void excludesSeenCandidates() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User seen = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User fresh = user(3L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(seen, fresh));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of(2L));

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.getCandidate().getUserId()).isEqualTo(3L);
        assertThat(response.isRecycled()).isFalse();
        verify(matchExposureRepository, never()).clear(anyLong());
    }

    @Test
    @DisplayName("모든 후보를 이미 봤으면 기록을 비우고 다시 순환한다")
    void allSeen_recycles() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User only = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(only));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of(2L));

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isRecycled()).isTrue();
        assertThat(response.getCandidate().getUserId()).isEqualTo(2L);
        verify(matchExposureRepository).clear(1L);
    }

    @Test
    @DisplayName("조건에 맞는 후보가 없으면 found=false와 안내 문구를 반환한다 (LOC-04)")
    void noCandidates_returnsGuide() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "경북 포항시")).willReturn(List.of());

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "경북 포항시");

        assertThat(response.isFound()).isFalse();
        assertThat(response.getCandidate()).isNull();
        assertThat(response.getGuide()).isNotBlank();
    }

    @Test
    @DisplayName("취향이 없으면 매칭하지 않고 안내한다")
    void noTags_returnsGuide() {
        User me = user(1L, "FEMALE", "대구 남구", null, null);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));

        RandomMatchResponse response = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(response.isFound()).isFalse();
        verify(matchCandidateRepository, never()).findAllCandidates(anyLong());
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
