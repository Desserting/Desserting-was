package com.desserting.backend.domain.matching.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 랜덤 매칭 중복 노출 방지용 Redis 저장소.
 * 키: match:seen:{userId} (Set), 첫 기록 후 24시간 뒤 자동 만료.
 */
@Repository
@RequiredArgsConstructor
public class MatchExposureRepository {

    private static final String KEY_PREFIX = "match:seen:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public Set<Long> findSeenUserIds(Long userId) {
        Set<String> members = redisTemplate.opsForSet().members(key(userId));
        if (members == null) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    public void markSeen(Long userId, Long candidateId) {
        String key = key(userId);
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(candidateId));
        Long ttl = redisTemplate.getExpire(key);
        if (added != null && (ttl == null || ttl < 0)) {
            redisTemplate.expire(key, TTL);
        }
    }

    public void clear(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
