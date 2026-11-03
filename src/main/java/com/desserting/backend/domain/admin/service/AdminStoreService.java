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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStoreService {

    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreReservationSlotRepository slotRepository;
    private final ThemeStreetRepository themeStreetRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public StoreAdminResponse createStore(StoreCreateRequest request) {
        ThemeStreet themeStreet = null;
        if (request.getThemeStreetId() != null) {
            themeStreet = themeStreetRepository.findById(request.getThemeStreetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));
        }

        Store store = Store.builder()
                .themeStreet(themeStreet)
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .businessHours(request.getBusinessHours())
                .contact(request.getContact())
                .coverImageUrl(request.getCoverImageUrl())
                .isPartner(Boolean.TRUE.equals(request.getIsPartner()))
                .partnerDiscountNotice(request.getPartnerDiscountNotice())
                .build();

        Store savedStore = storeRepository.save(store);
        log.info("새 제휴 매장 등록 완료: storeId={}, name={}, isPartner={}", savedStore.getId(), savedStore.getName(), savedStore.getIsPartner());
        return StoreAdminResponse.from(savedStore);
    }

    @Transactional(readOnly = true)
    public StoreAdminResponse getStore(Long storeId) {
        Store store = storeRepository.findByIdWithMenus(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        return StoreAdminResponse.from(store);
    }

    @Transactional(readOnly = true)
    public Page<StoreAdminResponse> getAllStores(Pageable pageable) {
        return storeRepository.findAll(pageable)
                .map(StoreAdminResponse::from);
    }

    @Transactional
    public StoreAdminResponse updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        ThemeStreet themeStreet = store.getThemeStreet();
        if (request.getThemeStreetId() != null) {
            themeStreet = themeStreetRepository.findById(request.getThemeStreetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));
        }

        store.updateInfo(
                request.getName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getBusinessHours(),
                request.getContact(),
                request.getCoverImageUrl(),
                themeStreet,
                request.getPartnerDiscountNotice()
        );

        log.info("매장 정보 수정 완료: storeId={}, name={}", store.getId(), store.getName());
        return StoreAdminResponse.from(store);
    }

    @Transactional
    public StoreAdminResponse updatePartnership(Long storeId, Boolean isPartner) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        store.updatePartnership(isPartner);
        log.info("매장 제휴 승인 상태 변경: storeId={}, isPartner={}", store.getId(), store.getIsPartner());
        return StoreAdminResponse.from(store);
    }

    @Transactional
    public StoreMenuResponse createMenu(Long storeId, StoreMenuCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        StoreMenu menu = StoreMenu.builder()
                .store(store)
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isRepresentative(Boolean.TRUE.equals(request.getIsRepresentative()))
                .isSoldOut(false)
                .build();

        StoreMenu savedMenu = storeMenuRepository.save(menu);
        log.info("매장 메뉴 등록 완료: storeId={}, menuId={}, name={}", storeId, savedMenu.getId(), savedMenu.getName());
        return StoreMenuResponse.from(savedMenu);
    }

    @Transactional
    public StoreMenuResponse updateMenu(Long storeId, Long menuId, StoreMenuUpdateRequest request) {
        StoreMenu menu = storeMenuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_MENU_NOT_FOUND));

        if (!menu.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.STORE_MENU_NOT_FOUND, "해당 매장의 메뉴가 아닙니다.");
        }

        menu.updateMenu(
                request.getName(),
                request.getPrice(),
                request.getDescription(),
                request.getImageUrl(),
                request.getIsRepresentative()
        );

        log.info("매장 메뉴 수정 완료: storeId={}, menuId={}, name={}", storeId, menu.getId(), menu.getName());
        return StoreMenuResponse.from(menu);
    }

    @Transactional
    public void deleteMenu(Long storeId, Long menuId) {
        StoreMenu menu = storeMenuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_MENU_NOT_FOUND));

        if (!menu.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.STORE_MENU_NOT_FOUND, "해당 매장의 메뉴가 아닙니다.");
        }

        storeMenuRepository.delete(menu);
        log.info("매장 메뉴 삭제 완료: storeId={}, menuId={}", storeId, menuId);
    }

    @Transactional
    public StoreMenuResponse toggleMenuSoldOut(Long storeId, Long menuId) {
        StoreMenu menu = storeMenuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_MENU_NOT_FOUND));

        if (!menu.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.STORE_MENU_NOT_FOUND, "해당 매장의 메뉴가 아닙니다.");
        }

        boolean isSoldOut = menu.toggleSoldOut();
        log.info("매장 메뉴 품절 토글 완료: storeId={}, menuId={}, isSoldOut={}", storeId, menuId, isSoldOut);
        return StoreMenuResponse.from(menu);
    }

    @Transactional
    public ReservationSlotResponse createReservationSlot(Long storeId, ReservationSlotCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        if (slotRepository.existsByStoreIdAndSlotDateAndSlotTime(storeId, request.getSlotDate(), request.getSlotTime())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION_SLOT);
        }

        StoreReservationSlot slot = StoreReservationSlot.builder()
                .store(store)
                .slotDate(request.getSlotDate())
                .slotTime(request.getSlotTime())
                .maxCapacity(request.getMaxCapacity())
                .currentReservedCount(0)
                .isAvailable(true)
                .build();

        StoreReservationSlot savedSlot = slotRepository.save(slot);
        log.info("예약 슬롯 개설 완료: storeId={}, date={}, time={}, max={}", storeId, slot.getSlotDate(), slot.getSlotTime(), slot.getMaxCapacity());
        return ReservationSlotResponse.from(savedSlot);
    }

    @Transactional(readOnly = true)
    public List<ReservationSlotResponse> getReservationSlots(Long storeId, LocalDate slotDate) {
        if (!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        List<StoreReservationSlot> slots = slotRepository.findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(storeId, slotDate);
        return slots.stream()
                .map(ReservationSlotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreSettlementResponse getSettlement(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 방문 완료(COMPLETED) 건수 및 총 결제 금액 동적 합산 집계
        long completedCount = reservationRepository.countByStoreIdAndStatus(storeId, ReservationStatus.COMPLETED);
        Long totalAmount = reservationRepository.sumTotalAmountByStoreIdAndStatus(storeId, ReservationStatus.COMPLETED);

        return StoreSettlementResponse.of(store.getId(), store.getName(), completedCount, totalAmount);
    }
}
