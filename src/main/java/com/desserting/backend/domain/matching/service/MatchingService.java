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
import com.desserting.backend.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 4-2. 랜덤 매칭·주변 사람 필터 (MAT-01~03, LOC-01~04)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    public static final double MIN_RADIUS_KM = 1;
    public static final double MAX_RADIUS_KM = 100;

    /** 서비스 전체 지역을 뜻하는 값. 이 값이면 지역 필터를 적용하지 않는다. */
    private static final Set<String> WHOLE_AREA_KEYWORDS = Set.of("전체", "대구·경북", "대구경북", "대구/경북");

    private final UserRepository userRepository;
    private final MatchCandidateRepository matchCandidateRepository;
    private final MatchExposureRepository matchExposureRepository;

    /**
     * 조건에 맞는 랜덤 상대 1명을 반환한다. 다시 호출하면 다음 상대(MAT-03).
     *
     * @param radiusKm 반경(km). 내 좌표가 없으면 지역 필터로 대체한다(LOC-03).
     * @param region   지역명 접두어(예: "대구", "대구 남구"). null/전체면 지역 무관.
     */
    @Transactional(readOnly = true)
    public RandomMatchResponse findRandomMatch(Long userId, MatchType matchType, Double radiusKm, String region) {
        if (matchType == null) {
            throw new ExploreException(ExploreErrorCode.INVALID_MATCH_FILTER, "매칭 타입(matchType)은 필수입니다.");
        }
        if (radiusKm != null && (radiusKm < MIN_RADIUS_KM || radiusKm > MAX_RADIUS_KM)) {
            throw new ExploreException(ExploreErrorCode.INVALID_MATCH_FILTER,
                    "거리는 " + (int) MIN_RADIUS_KM + "~" + (int) MAX_RADIUS_KM + "km 범위여야 합니다.");
        }

        User me = userRepository.findByIdWithTags(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Set<DessertType> myTags = tagsOf(me);

        if (myTags.isEmpty()) {
            return notFound(matchType, AppliedFilter.ALL, "디저트 취향을 먼저 선택해야 매칭을 시작할 수 있어요.");
        }

        boolean hasMyLocation = me.getLatitude() != null && me.getLongitude() != null;
        boolean useDistance = radiusKm != null && hasMyLocation;
        String regionPrefix = normalizeRegion(region);
        String guide = null;

        if (radiusKm != null && !hasMyLocation && regionPrefix == null) {
            // 위치 권한 미허용 시 내 활동 지역으로 대체 (LOC-03)
            regionPrefix = normalizeRegion(me.getRegion());
            guide = "위치 정보가 없어 내 활동 지역 기준으로 찾았어요.";
        }

        AppliedFilter appliedFilter;
        List<User> pool;
        if (useDistance) {
            appliedFilter = AppliedFilter.DISTANCE;
            pool = findWithinRadius(me, radiusKm);
        } else if (regionPrefix != null) {
            appliedFilter = AppliedFilter.REGION;
            pool = matchCandidateRepository.findCandidatesByRegion(userId, regionPrefix);
        } else {
            appliedFilter = AppliedFilter.ALL;
            pool = matchCandidateRepository.findAllCandidates(userId);
        }

        // 내가 차단했거나 나를 차단한 사용자는 제외 (SEC-01)
        Set<Long> blocked = new HashSet<>(matchCandidateRepository.findBlockRelatedUserIds(userId));
        List<User> candidates = pool.stream()
                .filter(c -> !blocked.contains(c.getId()))
                .filter(c -> isOppositeGender(me, c))
                .filter(c -> matchesTaste(matchType, myTags, tagsOf(c)))
                .toList();

        if (candidates.isEmpty()) {
            return notFound(matchType, appliedFilter, "조건에 맞는 상대가 없어요. 거리나 지역 범위를 넓혀보세요.");
        }

        Set<Long> seen = matchExposureRepository.findSeenUserIds(userId);
        List<User> unseen = candidates.stream().filter(c -> !seen.contains(c.getId())).toList();
        boolean recycled = false;
        if (unseen.isEmpty()) {
            // 오늘 후보를 모두 봤으면 기록을 비우고 다시 순환한다
            matchExposureRepository.clear(userId);
            unseen = candidates;
            recycled = true;
        }

        User picked = unseen.get(ThreadLocalRandom.current().nextInt(unseen.size()));
        matchExposureRepository.markSeen(userId, picked.getId());
        log.debug("랜덤 매칭: userId={}, type={}, filter={}, pool={}, picked={}",
                userId, matchType, appliedFilter, candidates.size(), picked.getId());

        return RandomMatchResponse.builder()
                .matchType(matchType)
                .found(true)
                .candidate(toCandidate(me, picked, myTags))
                .appliedFilter(appliedFilter)
                .recycled(recycled)
                .guide(guide)
                .build();
    }

    private List<User> findWithinRadius(User me, double radiusKm) {
        double lat = me.getLatitude().doubleValue();
        double lng = me.getLongitude().doubleValue();
        double dLat = GeoUtils.latDelta(radiusKm);
        double dLng = GeoUtils.lngDelta(radiusKm, lat);

        return matchCandidateRepository.findCandidatesInBox(me.getId(),
                        BigDecimal.valueOf(lat - dLat), BigDecimal.valueOf(lat + dLat),
                        BigDecimal.valueOf(lng - dLng), BigDecimal.valueOf(lng + dLng))
                .stream()
                .filter(c -> GeoUtils.distanceKm(me.getLatitude(), me.getLongitude(),
                        c.getLatitude(), c.getLongitude()) <= radiusKm)
                .toList();
    }

    static boolean matchesTaste(MatchType matchType, Set<DessertType> mine, Set<DessertType> theirs) {
        if (theirs.isEmpty()) {
            return false;
        }
        boolean overlaps = theirs.stream().anyMatch(mine::contains);
        return matchType == MatchType.SAME_TASTE ? overlaps : !overlaps;
    }

    /** 기획서 기준 이성 후보만 노출. 성별 미입력자는 제한하지 않는다. */
    static boolean isOppositeGender(User me, User candidate) {
        if (me.getGender() == null || candidate.getGender() == null) {
            return true;
        }
        return !me.getGender().equals(candidate.getGender());
    }

    static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        String trimmed = region.trim();
        return WHOLE_AREA_KEYWORDS.contains(trimmed) ? null : trimmed;
    }

    private MatchCandidateResponse toCandidate(User me, User c, Set<DessertType> myTags) {
        List<DessertType> theirTags = c.getDessertTags().stream().map(UserDessertTag::getDessertType).toList();

        Integer approxKm = null;
        if (me.getLatitude() != null && me.getLongitude() != null
                && c.getLatitude() != null && c.getLongitude() != null) {
            double km = GeoUtils.distanceKm(me.getLatitude(), me.getLongitude(), c.getLatitude(), c.getLongitude());
            // 정확한 위치 추정을 막기 위해 정수 km로 올림 (최소 1km)
            approxKm = Math.max(1, (int) Math.ceil(km));
        }

        return MatchCandidateResponse.builder()
                .userId(c.getId())
                .nickname(c.getNickname())
                .age(c.getAge())
                .gender(c.getGender())
                .region(c.getRegion())
                .bio(c.getBio())
                .dessertAvatarUrl(c.getDessertAvatarUrl())
                .dessertTags(theirTags)
                .commonDessertTags(theirTags.stream().filter(myTags::contains).toList())
                .approxDistanceKm(approxKm)
                .build();
    }

    private static Set<DessertType> tagsOf(User user) {
        return user.getDessertTags().stream().map(UserDessertTag::getDessertType).collect(Collectors.toSet());
    }

    private static RandomMatchResponse notFound(MatchType type, AppliedFilter filter, String guide) {
        return RandomMatchResponse.builder()
                .matchType(type)
                .found(false)
                .appliedFilter(filter)
                .guide(guide)
                .build();
    }

    /**
     * 4-2. 이전 매칭 상대(스킵한 상대) 목록 조회 (프리미엄 전용, MAT-04)
     */
    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> getSkippedMatchHistory(Long userId, int limit) {
        User me = userRepository.findByIdWithTags(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(me.getIsPremium())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "프리미엄 회원만 이전 매칭 상대 기록을 확인할 수 있습니다.");
        }

        int safeLimit = (limit <= 0 || limit > 50) ? 20 : limit;
        List<Long> candidateIds = matchExposureRepository.findHistoryUserIds(userId, safeLimit);
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Set<DessertType> myTags = tagsOf(me);
        List<User> candidates = userRepository.findAllById(candidateIds);
        Map<Long, User> userMap = candidates.stream().collect(Collectors.toMap(User::getId, u -> u));

        return candidateIds.stream()
                .filter(userMap::containsKey)
                .map(userMap::get)
                .map(candidate -> toCandidate(me, candidate, myTags))
                .toList();
    }
}
