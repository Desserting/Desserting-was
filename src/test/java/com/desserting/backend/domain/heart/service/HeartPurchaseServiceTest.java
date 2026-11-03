package com.desserting.backend.domain.heart.service;

import com.desserting.backend.domain.heart.dto.HeartPackageResponse;
import com.desserting.backend.domain.heart.dto.HeartPurchaseRequest;
import com.desserting.backend.domain.heart.dto.HeartPurchaseResponse;
import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import com.desserting.backend.domain.heart.entity.HeartPackage;
import com.desserting.backend.domain.heart.entity.HeartPurchase;
import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
import com.desserting.backend.domain.heart.repository.HeartPurchaseRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeartPurchaseServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private HeartHistoryRepository heartHistoryRepository;
    @Mock
    private HeartPurchaseRepository heartPurchaseRepository;

    private HeartPurchaseService service;

    @BeforeEach
    void setUp() {
        service = new HeartPurchaseService(userRepository, heartHistoryRepository, heartPurchaseRepository, 200);
    }

    @Test
    @DisplayName("상품 가격은 하트 수 × 200원")
    void packages_priceIs200PerHeart() {
        List<HeartPackageResponse> packages = service.getPackages();

        assertThat(packages).extracting(HeartPackageResponse::quantity).containsExactly(5, 10, 30);
        assertThat(packages).extracting(HeartPackageResponse::price).containsExactly(1000, 2000, 6000);
        assertThat(packages).allMatch(p -> p.unitPrice() == 200);
    }

    @Test
    @DisplayName("단가 설정을 바꾸면 가격도 바뀐다 (500원안 대비)")
    void packages_followConfiguredUnitPrice() {
        HeartPurchaseService price500 = new HeartPurchaseService(userRepository, heartHistoryRepository, heartPurchaseRepository, 500);
        assertThat(price500.getPackages()).extracting(HeartPackageResponse::price).containsExactly(2500, 5000, 15000);
    }

    @Test
    @DisplayName("구매 시 하트를 충전하고 PURCHASE 내역과 구매 기록을 남긴다")
    void purchase_addsHeartsAndRecords() {
        User user = user(1L, 3, false);
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(heartPurchaseRepository.save(any(HeartPurchase.class))).willAnswer(inv -> inv.getArgument(0));

        HeartPurchaseResponse response = service.purchase(1L, new HeartPurchaseRequest(HeartPackage.HEARTS_10, MockPaymentMethod.CARD));

        assertThat(user.getHeartCount()).isEqualTo(13);
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.amount()).isEqualTo(2000);
        assertThat(response.heartCountAfter()).isEqualTo(13);
        assertThat(response.paymentKey()).startsWith("MOCK-");

        ArgumentCaptor<HeartHistory> history = ArgumentCaptor.forClass(HeartHistory.class);
        verify(heartHistoryRepository).save(history.capture());
        assertThat(history.getValue().getType()).isEqualTo(HeartHistoryType.PURCHASE);
        assertThat(history.getValue().getAmount()).isEqualTo(10);
        assertThat(history.getValue().getBalanceAfter()).isEqualTo(13);
    }

    @Test
    @DisplayName("프리미엄 회원은 하트를 구매할 수 없다")
    void purchase_premiumRejected() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user(1L, 0, true)));

        assertThatThrownBy(() -> service.purchase(1L, new HeartPurchaseRequest(HeartPackage.HEARTS_5, MockPaymentMethod.CARD)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(heartPurchaseRepository, never()).save(any());
        verify(heartHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 사용자는 USER_NOT_FOUND")
    void purchase_userNotFound() {
        given(userRepository.findByIdWithLock(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(9L, new HeartPurchaseRequest(HeartPackage.HEARTS_5, MockPaymentMethod.EASY_PAY)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    private static User user(Long id, int hearts, boolean premium) {
        User user = User.builder().email("u@desserting.com").nickname("u").heartCount(hearts).isPremium(premium).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
