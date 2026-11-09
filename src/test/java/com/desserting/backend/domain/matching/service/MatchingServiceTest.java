package com.desserting.backend.domain.matching.service;

import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.explore.error.ExploreErrorCode;
import com.desserting.backend.domain.explore.error.ExploreException;
import com.desserting.backend.domain.matching.dto.MatchCandidateResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse.AppliedFilter;
import com.desserting.backend.domain.matching.entity.MatchType;
import com.desserting.backend.domain.matching.repository.ChatRequestRepository;
import com.desserting.backend.domain.matching.repository.MatchCandidateRepository;
import com.desserting.backend.domain.matching.repository.MatchExposureRepository;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserBlock;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserLikeRepository;
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
    @Mock
    private ChatRequestRepository chatRequestRepository;
    @Mock
    private UserLikeRepository userLikeRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private MatchingConfigService matchingConfigService;

    @InjectMocks
    private MatchingService matchingService;

    // 앞산 근처
    private static final double ME_LAT = 35.8290;
    private static final double ME_LNG = 128.5850;

    @Test
    @DisplayName("같은 취향 매칭은 겹치는 디저트가 있는 이성만 반환하고 노출 기록을 남긴다")
    void sameTaste_returnsOverlappingOppositeGender() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE, DessertType.COFFEE);
        User match = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE, DessertType.BAKERY);
        User noMatch = user(3L, "MALE", "대구 남구", null, null, DessertType.ICE_CREAM_BINGSU);
        User sameGender = user(4L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(match, noMatch, sameGender));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(res.getCandidate().getCommonDessertTags()).containsExactly(DessertType.CAKE);
        verify(matchExposureRepository).markSeen(1L, 2L);
    }

    @Test
    @DisplayName("다른 취향 매칭은 디저트가 겹치지 않는 이성만 반환한다")
    void differentTaste_returnsNonOverlappingOppositeGender() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User overlap = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE, DessertType.COFFEE);
        User diff = user(3L, "MALE", "대구 남구", null, null, DessertType.TRADITIONAL_KOREAN);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(overlap, diff));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.DIFFERENT_TASTE, null, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.getCandidate().getUserId()).isEqualTo(3L);
        assertThat(res.getCandidate().getCommonDessertTags()).isEmpty();
    }

    @Test
    @DisplayName("내 위치가 있고 거리 필터가 주어지면 반경 내 이성만 반환한다")
    void distanceFilter_usesGeoDistance() {
        User me = user(1L, "FEMALE", "대구 남구", ME_LAT, ME_LNG, DessertType.CAKE);
        // 약 2km 거리
        User near = user(2L, "MALE", "대구 남구", ME_LAT + 0.015, ME_LNG + 0.015, DessertType.CAKE);
        // 약 20km 거리 (수성구 끝) - 박스 쿼리에서 걸러지거나 거리 계산에서 걸러진다
        User far = user(3L, "MALE", "대구 수성구", ME_LAT + 0.18, ME_LNG + 0.18, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesInBox(eq(1L), any(), any(), any(), any()))
                .willReturn(List.of(near, far));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 5.0, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.getAppliedFilter()).isEqualTo(AppliedFilter.DISTANCE);
        assertThat(res.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(res.getCandidate().getApproxDistanceKm()).isNotNull();
    }

    @Test
    @DisplayName("내 위치 정보가 없을 때 거리 필터를 요청하면 내 활동 지역으로 자동 대체된다 (LOC-03)")
    void fallbackToRegion_whenLocationMissing() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User candidate = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "대구 남구")).willReturn(List.of(candidate));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 5.0, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
        assertThat(res.getGuide()).contains("내 활동 지역");
        assertThat(res.getCandidate().getUserId()).isEqualTo(2L);
        assertThat(res.getCandidate().getApproxDistanceKm()).isNull();
    }

    @Test
    @DisplayName("지역 필터가 주어지면 해당 지역 후보만 반환한다")
    void regionFilter_usesExactPrefix() {
        User me = user(1L, "FEMALE", "대구 중구", null, null, DessertType.CAKE);
        User candidate = user(2L, "MALE", "대구 중구 삼덕동", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findCandidatesByRegion(1L, "대구 중구")).willReturn(List.of(candidate));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구 중구");

        assertThat(res.isFound()).isTrue();
        assertThat(res.getAppliedFilter()).isEqualTo(AppliedFilter.REGION);
        assertThat(res.getCandidate().getUserId()).isEqualTo(2L);
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
    @DisplayName("지역 키워드가 '전체' 또는 '대구·경북'이면 지역 필터를 무시하고 전체 풀을 조회한다")
    void wholeAreaKeywords_ignoreRegionFilter() {
        User me = user(1L, "FEMALE", "대구", null, null, DessertType.CAKE);
        User candidate = user(2L, "MALE", "구미", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(candidate));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, "대구·경북");

        assertThat(res.isFound()).isTrue();
        assertThat(res.getAppliedFilter()).isEqualTo(AppliedFilter.ALL);
        verify(matchCandidateRepository, never()).findCandidatesByRegion(anyLong(), any());
    }

    @Test
    @DisplayName("매칭 필터 반경 범위가 1~100km를 벗어나면 400 예외가 발생한다")
    void invalidRadius_throwsBadRequest() {
        assertThatThrownBy(() -> matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 0.5, null))
                .isInstanceOf(ExploreException.class)
                .extracting("errorCode").isEqualTo(ExploreErrorCode.INVALID_MATCH_FILTER);

        assertThatThrownBy(() -> matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, 150.0, null))
                .isInstanceOf(ExploreException.class)
                .extracting("errorCode").isEqualTo(ExploreErrorCode.INVALID_MATCH_FILTER);
    }

    @Test
    @DisplayName("모든 후보를 다 본 경우 노출 기록을 비우고 다시 순환한다 (recycled=true)")
    void allSeen_clearsExposureAndRecycles() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User onlyOne = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(onlyOne));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        // 이미 2번 유저를 봤음
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of(2L));

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.isRecycled()).isTrue();
        verify(matchExposureRepository).clear(1L);
        verify(matchExposureRepository).markSeen(1L, 2L);
    }

    @Test
    @DisplayName("차단 관계인 상대는 후보에서 제외한다 (SEC-01)")
    void blockedUsers_excludedFromCandidates() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User blocked = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(blocked));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of(2L));

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(res.isFound()).isFalse();
        assertThat(res.getCandidate()).isNull();
    }

    @Test
    @DisplayName("취향 태그가 없는 사용자는 매칭을 시도하면 가이드와 함께 후보 없음을 반환한다")
    void noTasteTags_returnsNotFoundWithGuide() {
        User me = user(1L, "FEMALE", "대구 남구", null, null);
        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(res.isFound()).isFalse();
        assertThat(res.getGuide()).contains("취향");
    }

    @Test
    @DisplayName("프리미엄 회원은 스킵한 상대 이력을 Fetch Join으로 일괄 조회하여 성공적으로 반환한다")
    void getSkippedMatchHistory_premiumUser_success() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        ReflectionTestUtils.setField(me, "isPremium", true);

        User candidate1 = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User candidate2 = user(3L, "MALE", "대구 수성구", null, null, DessertType.COFFEE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchExposureRepository.findHistoryUserIds(1L, 20)).willReturn(List.of(2L, 3L));
        given(userRepository.findAllByIdInWithTags(List.of(2L, 3L))).willReturn(List.of(candidate1, candidate2));

        List<MatchCandidateResponse> history = matchingService.getSkippedMatchHistory(1L, 20);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getUserId()).isEqualTo(2L);
        assertThat(history.get(1).getUserId()).isEqualTo(3L);
        verify(userRepository).findAllByIdInWithTags(List.of(2L, 3L));
    }

    @Test
    @DisplayName("이미 활성 채팅 중인 상대는 랜덤 매칭 후보에서 제외된다")
    void findRandomMatch_excludesActiveChatPartners() {
        User me = user(1L, "FEMALE", "대구 남구", null, null, DessertType.CAKE);
        User activePartner = user(2L, "MALE", "대구 남구", null, null, DessertType.CAKE);
        User newCandidate = user(3L, "MALE", "대구 남구", null, null, DessertType.CAKE);

        given(userRepository.findByIdWithTags(1L)).willReturn(Optional.of(me));
        given(matchCandidateRepository.findAllCandidates(1L)).willReturn(List.of(activePartner, newCandidate));
        given(matchCandidateRepository.findBlockRelatedUserIds(1L)).willReturn(List.of());
        given(chatRoomMemberRepository.findActivePartnerUserIds(1L)).willReturn(List.of(2L));
        given(matchExposureRepository.findSeenUserIds(1L)).willReturn(Set.of());

        RandomMatchResponse res = matchingService.findRandomMatch(1L, MatchType.SAME_TASTE, null, null);

        assertThat(res.isFound()).isTrue();
        assertThat(res.getCandidate().getUserId()).isEqualTo(3L);
        verify(matchExposureRepository).markSeen(1L, 3L);
    }

    private static User user(Long id, String gender, String region, Double lat, Double lng, DessertType... tags) {
        User.UserBuilder builder = User.builder()
                .email("u" + id + "@test.com")
                .password("encoded")
                .nickname("nick" + id)
                .gender(gender)
                .region(region)
                .age(25);
        if (lat != null && lng != null) {
            builder.latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lng));
        }
        User u = builder.build();
        ReflectionTestUtils.setField(u, "id", id);
        List<UserDessertTag> tagList = new ArrayList<>();
        for (DessertType t : tags) {
            tagList.add(UserDessertTag.builder().user(u).dessertType(t).build());
        }
        ReflectionTestUtils.setField(u, "dessertTags", tagList);
        return u;
    }
}
