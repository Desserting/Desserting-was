package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.*;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.domain.reservation.repository.ReservationRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import com.desserting.backend.domain.store.repository.StoreMenuRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.store.repository.StoreReservationSlotRepository;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Mock
    private StoreReservationSlotRepository slotRepository;

    @Mock
    private ThemeStreetRepository themeStreetRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private com.desserting.backend.domain.store.repository.StoreCouponRepository storeCouponRepository;

    @InjectMocks
    private AdminStoreService adminStoreService;

    @Test
    @DisplayName("신규 매장 등록 시 정상 저장 및 응답 반환")
    void createStore_Success() {
        // given
        ThemeStreet themeStreet = ThemeStreet.builder().name("앞산 카페거리").build();
        ReflectionTestUtils.setField(themeStreet, "id", 1L);

        StoreCreateRequest request = StoreCreateRequest.builder()
                .themeStreetId(1L)
                .name("카페 루시드")
                .address("대구 중구 동성로4길 94")
                .latitude(new BigDecimal("35.8687000"))
                .longitude(new BigDecimal("128.5982000"))
                .businessHours("12:00-21:30")
                .contact("053-422-7020")
                .partnerDiscountNotice("10% 할인")
                .isPartner(true)
                .build();

        given(themeStreetRepository.findById(1L)).willReturn(Optional.of(themeStreet));
        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> {
            Store s = invocation.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 10L);
            return s;
        });

        // when
        StoreAdminResponse response = adminStoreService.createStore(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("카페 루시드");
        assertThat(response.getIsPartner()).isTrue();
        assertThat(response.getThemeStreetName()).isEqualTo("앞산 카페거리");
    }

    @Test
    @DisplayName("매장 제휴 승인 상태 변경 성공")
    void updatePartnership_Success() {
        // given
        Store store = Store.builder().name("테스트카페").isPartner(false).build();
        ReflectionTestUtils.setField(store, "id", 10L);

        given(storeRepository.findById(10L)).willReturn(Optional.of(store));

        // when
        StoreAdminResponse response = adminStoreService.updatePartnership(10L, true);

        // then
        assertThat(response.getIsPartner()).isTrue();
        assertThat(store.getIsPartner()).isTrue();
    }

    @Test
    @DisplayName("디저트 메뉴 등록 및 품절 토글 성공")
    void menu_CreateAndToggleSoldOut() {
        // given
        Store store = Store.builder().name("카페 루시드").menus(new ArrayList<>()).build();
        ReflectionTestUtils.setField(store, "id", 10L);

        StoreMenu menu = StoreMenu.builder()
                .store(store)
                .name("푸딩 빙수")
                .price(18000)
                .isRepresentative(true)
                .isSoldOut(false)
                .build();
        ReflectionTestUtils.setField(menu, "id", 100L);

        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(storeMenuRepository.save(any(StoreMenu.class))).willReturn(menu);
        given(storeMenuRepository.findById(100L)).willReturn(Optional.of(menu));

        // when (등록)
        StoreMenuCreateRequest createReq = StoreMenuCreateRequest.builder()
                .name("푸딩 빙수")
                .price(18000)
                .isRepresentative(true)
                .build();
        StoreMenuResponse createRes = adminStoreService.createMenu(10L, createReq);

        // then (등록 검증)
        assertThat(createRes.getMenuId()).isEqualTo(100L);
        assertThat(createRes.getIsSoldOut()).isFalse();

        // when (품절 토글)
        StoreMenuResponse toggleRes = adminStoreService.toggleMenuSoldOut(10L, 100L);

        // then (품절 확인)
        assertThat(toggleRes.getIsSoldOut()).isTrue();
    }

    @Test
    @DisplayName("예약 슬롯 개설 시 동일 시간대 중복 슬롯 방지 검증")
    void createReservationSlot_DuplicateError() {
        // given
        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", 10L);

        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(14, 0);

        ReservationSlotCreateRequest request = ReservationSlotCreateRequest.builder()
                .slotDate(date)
                .slotTime(time)
                .maxCapacity(4)
                .build();

        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(slotRepository.existsByStoreIdAndSlotDateAndSlotTime(10L, date, time)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminStoreService.createReservationSlot(10L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESERVATION_SLOT);
    }

    @Test
    @DisplayName("7% 플랫폼 수수료 및 매장 정산액 동적 계산 검증 (SELL-08 단디톤 핵심 스펙)")
    void getSettlement_DynamicCalculation() {
        // given
        Store store = Store.builder().name("카페 루시드").build();
        ReflectionTestUtils.setField(store, "id", 10L);

        long completedCount = 5L;
        long totalOrderAmount = 100_000L; // 10만원 매출

        given(storeRepository.findById(10L)).willReturn(Optional.of(store));
        given(reservationRepository.countByStoreIdAndStatus(10L, ReservationStatus.COMPLETED)).willReturn(completedCount);
        given(reservationRepository.sumTotalAmountByStoreIdAndStatus(10L, ReservationStatus.COMPLETED)).willReturn(totalOrderAmount);

        // when
        StoreSettlementResponse response = adminStoreService.getSettlement(10L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(10L);
        assertThat(response.getCompletedOrderCount()).isEqualTo(5L);
        assertThat(response.getTotalOrderAmount()).isEqualTo(100_000L);
        assertThat(response.getPlatformFeeRate()).isEqualTo(7.0);
        assertThat(response.getPlatformFeeAmount()).isEqualTo(7_000L); // 100,000 * 0.07 = 7,000원
        assertThat(response.getSettlementAmount()).isEqualTo(93_000L);   // 100,000 - 7,000 = 93,000원
    }

    @Test
    @DisplayName("방문 완료 주문이 없는 매장(0건, null 금액)의 경우 정산금이 0원으로 안전하게 계산된다")
    void getSettlement_ZeroOrNullCompletedOrders() {
        // given
        Store store = Store.builder().name("신규 디저트 매장").build();
        ReflectionTestUtils.setField(store, "id", 20L);

        given(storeRepository.findById(20L)).willReturn(Optional.of(store));
        given(reservationRepository.countByStoreIdAndStatus(20L, ReservationStatus.COMPLETED)).willReturn(0L);
        given(reservationRepository.sumTotalAmountByStoreIdAndStatus(20L, ReservationStatus.COMPLETED)).willReturn(null);

        // when
        StoreSettlementResponse response = adminStoreService.getSettlement(20L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(20L);
        assertThat(response.getCompletedOrderCount()).isEqualTo(0L);
        assertThat(response.getTotalOrderAmount()).isEqualTo(0L);
        assertThat(response.getPlatformFeeAmount()).isEqualTo(0L);
        assertThat(response.getSettlementAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("매장 자율 할인쿠폰 등록 성공 및 기본 활성화(isActive=true) 검증")
    void createCoupon_Success() {
        // given
        Store store = Store.builder().name("아눅 앞산").build();
        ReflectionTestUtils.setField(store, "id", 1L);

        StoreCouponCreateRequest request = new StoreCouponCreateRequest("오픈기념 2천원 할인", 2000, 10000, LocalDate.now().plusMonths(1));
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));
        given(storeCouponRepository.save(any(com.desserting.backend.domain.store.entity.StoreCoupon.class))).willAnswer(inv -> {
            com.desserting.backend.domain.store.entity.StoreCoupon c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 100L);
            return c;
        });

        // when
        com.desserting.backend.domain.store.dto.StoreCouponResponse response = adminStoreService.createCoupon(1L, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.couponId()).isEqualTo(100L);
        assertThat(response.couponName()).isEqualTo("오픈기념 2천원 할인");
        assertThat(response.discountAmount()).isEqualTo(2000);
        assertThat(response.isActive()).isTrue();
    }
}
