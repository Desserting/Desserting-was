package com.desserting.backend.domain.matching.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 랜덤 매칭 중복 노출 방지 및 이전 스킵 상대 이력용 Redis 저장소.
 * - Set: match:seen:{userId} (오늘 본 상대 집합)
 * - ZSet: match:history:{userId} (최신순 스킵/매칭 이력)
 */
@Repository
@RequiredArgsConstructor
public class MatchExposureRepository {

    private static final String KEY_PREFIX = "match:seen:";
    private static final String HISTORY_PREFIX = "match:history:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public Set<Long> findSeenUserIds(Long userId) {
        Set<String> members = redisTemplate.opsForSet().members(key(userId));
        if (members == null) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    public List<Long> findHistoryUserIds(Long userId, int limit) {
        Set<String> history = redisTemplate.opsForZSet().reverseRangeByScore(historyKey(userId),
                windowStart(), Double.POSITIVE_INFINITY, 0, limit);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream().map(Long::valueOf).toList();
    }

    public void markSeen(Long userId, Long candidateId) {
        String key = key(userId);
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(candidateId));
        Long ttl = redisTemplate.getExpire(key);
        if (added != null && (ttl == null || ttl < 0)) {
            redisTemplate.expire(key, TTL);
        }

        // ZSet에도 최신 타임스탬프와 함께 보관
        String hKey = historyKey(userId);
        redisTemplate.opsForZSet().add(hKey, String.valueOf(candidateId), System.currentTimeMillis());
        // 이력은 마지막 노출 기준 24시간 보관하고, 조회 시 점수(노출 시각)로 24시간 이내만 거른다
        redisTemplate.expire(hKey, TTL);
        redisTemplate.opsForZSet().removeRangeByScore(hKey, Double.NEGATIVE_INFINITY, windowStart() - 1);
    }

    /** 최근 24시간 안에 매칭으로 보여준 상대인지 (일반 회원 대화 요청 가능 여부) */
    public boolean wasShownRecently(Long userId, Long candidateId) {
        Double shownAt = redisTemplate.opsForZSet().score(historyKey(userId), String.valueOf(candidateId));
        return shownAt != null && shownAt >= windowStart();
    }

    private double windowStart() {
        return System.currentTimeMillis() - TTL.toMillis();
    }

    public void clear(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private String historyKey(Long userId) {
        return HISTORY_PREFIX + userId;
    }
}
