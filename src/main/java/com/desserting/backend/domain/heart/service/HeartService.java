package com.desserting.backend.domain.heart.service;

import com.desserting.backend.domain.heart.dto.HeartHistoryResponse;
import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartService {

    private final UserRepository userRepository;
    private final HeartHistoryRepository heartHistoryRepository;

    @Transactional
    public User deductHeartForRequest(Long userId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsPremium())) {
            log.info("프리미엄 회원 대화 요청: userId={}, 하트 차감 면제", userId);
            return user;
        }

        if (user.getHeartCount() < 1) {
            throw new CustomException(ErrorCode.INSUFFICIENT_HEART);
        }

        user.deductHeart(1);
        heartHistoryRepository.save(HeartHistory.of(
                user,
                -1,
                user.getHeartCount(),
                HeartHistoryType.REQUEST_DEDUCT,
                "대화 요청 전송 하트 1개 차감"
        ));
        log.info("하트 차감 완료: userId={}, 잔여 하트={}", userId, user.getHeartCount());
        return user;
    }

    @Transactional
    public void refundHeart(Long userId, String reason) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsPremium())) {
            log.info("프리미엄 회원 대화 요청 환불 제외 (원래 미차감): userId={}", userId);
            return;
        }

        user.addHeart(1);
        heartHistoryRepository.save(HeartHistory.of(
                user,
                1,
                user.getHeartCount(),
                HeartHistoryType.REQUEST_REFUND,
                reason
        ));
        log.info("하트 환불 완료: userId={}, 사유={}, 잔여 하트={}", userId, reason, user.getHeartCount());
    }

    @Transactional
    public int chargeDailyHearts() {
        int updatedCount = userRepository.resetDailyHeartsForUsers();
        log.info("자정 무료 하트 충전 완료: {}명의 사용자에게 5개 하트 리셋", updatedCount);
        return updatedCount;
    }

    @Transactional(readOnly = true)
    public List<HeartHistoryResponse> getMyHeartHistories(Long userId) {
        List<HeartHistory> histories = heartHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return histories.stream()
                .map(HeartHistoryResponse::from)
                .toList();
    }
}
