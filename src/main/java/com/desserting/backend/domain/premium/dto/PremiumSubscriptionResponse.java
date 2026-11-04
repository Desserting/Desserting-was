package com.desserting.backend.domain.premium.dto;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.premium.entity.PremiumSubscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "프리미엄 패스 결제 결과")
public record PremiumSubscriptionResponse(
        @Schema(example = "1") Long subscriptionId,
        @Schema(example = "9900") Integer amount,
        @Schema(example = "CARD") MockPaymentMethod paymentMethod,
        @Schema(description = "모의 결제 승인 번호", example = "MOCK-PRE-...") String paymentKey,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {
    public static PremiumSubscriptionResponse from(PremiumSubscription s) {
        return new PremiumSubscriptionResponse(s.getId(), s.getAmount(), s.getPaymentMethod(), s.getPaymentKey(),
                s.getStartedAt(), s.getExpiresAt());
    }
}
