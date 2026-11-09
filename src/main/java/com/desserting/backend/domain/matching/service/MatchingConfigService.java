package com.desserting.backend.domain.matching.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingConfigService {

    private static final String DUMMY_MATCH_ENABLED_KEY = "admin:config:dummy_match_enabled";
    private final StringRedisTemplate redisTemplate;

    /**
     * 더미 유저 매칭 활성화 여부 조회 (기본값: true)
     */
    public boolean isDummyMatchEnabled() {
        String val = redisTemplate.opsForValue().get(DUMMY_MATCH_ENABLED_KEY);
        if (val == null) {
            return true; // 기본 활성화
        }
        return Boolean.parseBoolean(val);
    }

    /**
     * 더미 유저 매칭 활성화 여부 변경
     */
    public boolean setDummyMatchEnabled(boolean enabled) {
        redisTemplate.opsForValue().set(DUMMY_MATCH_ENABLED_KEY, String.valueOf(enabled));
        return enabled;
    }

    /**
     * 토글 (현재 상태 반전)
     */
    public boolean toggleDummyMatch() {
        boolean next = !isDummyMatchEnabled();
        setDummyMatchEnabled(next);
        return next;
    }
}
