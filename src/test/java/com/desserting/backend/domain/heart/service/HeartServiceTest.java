package com.desserting.backend.domain.heart.service;

import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeartServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HeartHistoryRepository heartHistoryRepository;

    @InjectMocks
    private HeartService heartService;

    @Test
    @DisplayName("일반 회원이 대화 요청 시 하트 1개가 차감되고 내역이 저장된다")
    void deductHeartForRequest_NormalUser() {
        // given
        User user = User.builder()
                .email("user@desserting.com")
                .nickname("딸기케이크")
                .heartCount(5)
                .isPremium(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));

        // when
        heartService.deductHeartForRequest(1L);

        // then
        assertThat(user.getHeartCount()).isEqualTo(4);
        verify(heartHistoryRepository).save(any(HeartHistory.class));
    }

    @Test
    @DisplayName("프리미엄 회원은 대화 요청 시 하트 차감이 면제된다")
    void deductHeartForRequest_PremiumUser() {
        // given
        User premiumUser = User.builder()
                .email("premium@desserting.com")
                .nickname("초코크로플")
                .heartCount(1)
                .isPremium(true)
                .build();
        ReflectionTestUtils.setField(premiumUser, "id", 2L);

        given(userRepository.findByIdWithLock(2L)).willReturn(Optional.of(premiumUser));

        // when
        heartService.deductHeartForRequest(2L);

        // then
        assertThat(premiumUser.getHeartCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("하트가 0개인 일반 회원이 요청 시 INSUFFICIENT_HEART 예외 발생")
    void deductHeartForRequest_InsufficientHeart() {
        // given
        User user = User.builder()
                .email("empty@desserting.com")
                .nickname("빈지갑")
                .heartCount(0)
                .isPremium(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);

        given(userRepository.findByIdWithLock(3L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> heartService.deductHeartForRequest(3L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_HEART);
    }

    @Test
    @DisplayName("대화 요청 거절 시 하트 1개가 환불된다")
    void refundHeart() {
        // given
        User user = User.builder()
                .email("user@desserting.com")
                .nickname("마카롱")
                .heartCount(2)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));

        // when
        heartService.refundHeart(1L, "요청 거절 환불");

        // then
        assertThat(user.getHeartCount()).isEqualTo(3);
        verify(heartHistoryRepository).save(any(HeartHistory.class));
    }
}
