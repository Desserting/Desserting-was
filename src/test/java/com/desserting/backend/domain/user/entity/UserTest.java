package com.desserting.backend.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("일반 회원의 하트 차감 및 부족 시 예외 발생 검증")
    void deductHeart_NormalUser() {
        // given
        User user = User.builder()
                .email("test@desserting.com")
                .password("encoded_pass")
                .nickname("딸기케이크")
                .heartCount(2)
                .isPremium(false)
                .build();

        // when
        user.deductHeart(1);

        // then
        assertThat(user.getHeartCount()).isEqualTo(1);

        // when & then: 2개 차감 시도 시 잔여 부족 예외
        assertThatThrownBy(() -> user.deductHeart(2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("보유 하트가 부족합니다.");
    }

    @Test
    @DisplayName("프리미엄 회원은 하트가 차감되지 않음")
    void deductHeart_PremiumUser() {
        // given
        User user = User.builder()
                .email("premium@desserting.com")
                .password("encoded_pass")
                .nickname("초코크로와상")
                .heartCount(0)
                .isPremium(true)
                .build();

        // when
        user.deductHeart(5);

        // then
        assertThat(user.getHeartCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("매일 무료 하트 충전 시 기존 하트가 5개 미만이면 5개로 초기화")
    void resetDailyHearts() {
        // given
        User user = User.builder()
                .email("user@desserting.com")
                .password("encoded_pass")
                .nickname("마카롱")
                .heartCount(2)
                .build();

        // when
        user.resetDailyHearts(5);

        // then
        assertThat(user.getHeartCount()).isEqualTo(5);
    }
}
