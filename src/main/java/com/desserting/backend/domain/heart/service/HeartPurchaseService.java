package com.desserting.backend.domain.heart.service;

import com.desserting.backend.domain.heart.dto.HeartPackageResponse;
import com.desserting.backend.domain.heart.dto.HeartPurchaseRequest;
import com.desserting.backend.domain.heart.dto.HeartPurchaseResponse;
import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import com.desserting.backend.domain.heart.entity.HeartPackage;
import com.desserting.backend.domain.heart.entity.HeartPurchase;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
import com.desserting.backend.domain.heart.repository.HeartPurchaseRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * HRT-05 하트 추가 구매 (모의 결제).
 * 단가는 기획 확정 전까지 설정값으로 둔다: desserting.heart.unit-price (기본 200원).
 */
@Slf4j
@Service
public class HeartPurchaseService {

    private final UserRepository userRepository;
    private final HeartHistoryRepository heartHistoryRepository;
    private final HeartPurchaseRepository heartPurchaseRepository;
    private final int unitPrice;

    public HeartPurchaseService(UserRepository userRepository,
                                HeartHistoryRepository heartHistoryRepository,
                                HeartPurchaseRepository heartPurchaseRepository,
                                @Value("${desserting.heart.unit-price:200}") int unitPrice) {
        this.userRepository = userRepository;
        this.heartHistoryRepository = heartHistoryRepository;
        this.heartPurchaseRepository = heartPurchaseRepository;
        this.unitPrice = unitPrice;
    }

    public List<HeartPackageResponse> getPackages() {
        return Arrays.stream(HeartPackage.values())
                .map(p -> HeartPackageResponse.of(p, unitPrice))
                .toList();
    }

    @Transactional
    public HeartPurchaseResponse purchase(Long userId, HeartPurchaseRequest request) {
        // 대화 요청 차감과 같은 비관적 락으로 잔액 갱신 충돌 방지
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsPremium())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                    "프리미엄 회원은 하트 차감 없이 대화 요청할 수 있어 하트를 구매할 필요가 없습니다.");
        }

        HeartPackage heartPackage = request.packageCode();
        int quantity = heartPackage.getQuantity();
        int amount = heartPackage.priceOf(unitPrice);

        user.addHeart(quantity);
        heartHistoryRepository.save(HeartHistory.of(
                user, quantity, user.getHeartCount(), HeartHistoryType.PURCHASE,
                String.format("하트 %d개 구매 (%,d원, %s)", quantity, amount, request.paymentMethod().getDescription())));

        HeartPurchase purchase = heartPurchaseRepository.save(HeartPurchase.builder()
                .user(user)
                .heartPackage(heartPackage)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .paymentMethod(request.paymentMethod())
                .paymentKey("MOCK-" + UUID.randomUUID())
                .heartCountAfter(user.getHeartCount())
                .build());

        log.info("하트 구매 완료: userId={}, package={}, amount={}원, 잔여 하트={}",
                userId, heartPackage, amount, user.getHeartCount());
        return HeartPurchaseResponse.from(purchase);
    }

    @Transactional(readOnly = true)
    public List<HeartPurchaseResponse> getMyPurchases(Long userId) {
        return heartPurchaseRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(HeartPurchaseResponse::from)
                .toList();
    }
}
