package com.desserting.backend.domain.premium.service;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PremiumServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 12, 0);

    @Mock private UserRepository userRepository;
    @Mock private PremiumUserRepository premiumUserRepository;
    @Mock private PremiumSubscriptionRepository subscriptionRepository;

    private PremiumService service;

    @BeforeEach
    void setUp() {
        service = new PremiumService(userRepository, premiumUserRepository, subscriptionRepository,
                Clock.fixed(NOW.atZone(KST).toInstant(), KST), 9900, 30);
    }

    @Test
    @DisplayName("일반 회원이 결제하면 9,900원·30일 이용권이 저장되고 프리미엄으로 바뀐다")
    void subscribe() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user(false)));
        given(subscriptionRepository.save(any(PremiumSubscription.class))).willAnswer(inv -> inv.getArgument(0));

        PremiumSubscriptionResponse res = service.subscribe(1L, new PremiumSubscribeRequest(MockPaymentMethod.CARD));

        assertThat(res.amount()).isEqualTo(9900);
        assertThat(res.startedAt()).isEqualTo(NOW);
        assertThat(res.expiresAt()).isEqualTo(NOW.plusDays(30));
        assertThat(res.paymentKey()).startsWith("MOCK-PRE-");
        verify(premiumUserRepository).activatePremium(1L);
    }

    @Test
    @DisplayName("이미 프리미엄이면 C001 로 거절하고 결제하지 않는다")
    void subscribe_alreadyPremium() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> service.subscribe(1L, new PremiumSubscribeRequest(MockPaymentMethod.CARD)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("상태 조회: 가격·혜택과, 프리미엄이면 만료 시각을 준다")
    void status() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(true)));
        given(subscriptionRepository.findFirstByUserIdOrderByExpiresAtDesc(1L)).willReturn(Optional.of(
                PremiumSubscription.builder().expiresAt(NOW.plusDays(3)).build()));

        PremiumStatusResponse res = service.getStatus(1L);

        assertThat(res.premium()).isTrue();
        assertThat(res.expiresAt()).isEqualTo(NOW.plusDays(3));
        assertThat(res.monthlyPrice()).isEqualTo(9900);
        assertThat(res.benefits()).isNotEmpty();
    }

    @Test
    @DisplayName("만료 처리는 현재 시각 기준으로 위임한다")
    void expire() {
        given(premiumUserRepository.expirePremium(NOW)).willReturn(2);
        assertThat(service.expireEndedSubscriptions()).isEqualTo(2);
    }

    private User user(boolean premium) {
        User u = User.builder().nickname("u").isPremium(premium).build();
        ReflectionTestUtils.setField(u, "id", 1L);
        return u;
    }
}
