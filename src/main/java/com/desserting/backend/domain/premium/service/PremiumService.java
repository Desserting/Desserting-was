package com.desserting.backend.domain.premium.service;

import com.desserting.backend.domain.premium.dto.PremiumStatusResponse;
import com.desserting.backend.domain.premium.dto.PremiumSubscribeRequest;
import com.desserting.backend.domain.premium.dto.PremiumSubscriptionResponse;
import com.desserting.backend.domain.premium.entity.PremiumSubscription;
import com.desserting.backend.domain.premium.repository.PremiumSubscriptionRepository;
import com.desserting.backend.domain.premium.repository.PremiumUserRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PRE-01 프리미엄 패스 (월 9,900원, 모의 결제).
 * 가격·기간은 설정값: desserting.premium.monthly-price (기본 9900), desserting.premium.period-days (기본 30).
 */
@Slf4j
@Service
public class PremiumService {

    private final UserRepository userRepository;
    private final PremiumUserRepository premiumUserRepository;
    private final PremiumSubscriptionRepository subscriptionRepository;
    private final Clock clock;
    private final int monthlyPrice;
    private final int periodDays;

    public PremiumService(UserRepository userRepository,
                          PremiumUserRepository premiumUserRepository,
                          PremiumSubscriptionRepository subscriptionRepository,
                          Clock clock,
                          @Value("${desserting.premium.monthly-price:9900}") int monthlyPrice,
                          @Value("${desserting.premium.period-days:30}") int periodDays) {
        this.userRepository = userRepository;
        this.premiumUserRepository = premiumUserRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
        this.monthlyPrice = monthlyPrice;
        this.periodDays = periodDays;
    }

    @Transactional(readOnly = true)
    public PremiumStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean premium = Boolean.TRUE.equals(user.getIsPremium());
        LocalDateTime expiresAt = premium
                ? subscriptionRepository.findFirstByUserIdOrderByExpiresAtDesc(userId)
                        .map(PremiumSubscription::getExpiresAt).orElse(null)
                : null;
        return new PremiumStatusResponse(premium, expiresAt, monthlyPrice, periodDays, PremiumStatusResponse.BENEFITS);
    }

    @Transactional
    public PremiumSubscriptionResponse subscribe(Long userId, PremiumSubscribeRequest request) {
        // 하트 구매와 같은 비관적 락으로 중복 결제 방지
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsPremium())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 프리미엄 패스를 이용 중입니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        PremiumSubscription subscription = subscriptionRepository.save(PremiumSubscription.builder()
                .user(user)
                .amount(monthlyPrice)
                .paymentMethod(request.paymentMethod())
                .paymentKey("MOCK-PRE-" + UUID.randomUUID())
                .startedAt(now)
                .expiresAt(now.plusDays(periodDays))
                .build());
        premiumUserRepository.activatePremium(userId);

        log.info("프리미엄 패스 결제 완료: userId={}, amount={}원, expiresAt={}", userId, monthlyPrice, subscription.getExpiresAt());
        return PremiumSubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public List<PremiumSubscriptionResponse> getMySubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(PremiumSubscriptionResponse::from)
                .toList();
    }

    /** 이용 기간이 끝난 결제 회원을 일반 회원으로 되돌린다 */
    @Transactional
    public int expireEndedSubscriptions() {
        return premiumUserRepository.expirePremium(LocalDateTime.now(clock));
    }
}
