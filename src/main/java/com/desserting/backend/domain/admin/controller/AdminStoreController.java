package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.domain.admin.dto.*;
import com.desserting.backend.domain.admin.service.AdminStoreService;
import com.desserting.backend.domain.store.dto.StoreCouponResponse;
import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "4-8. 매장 관리자 및 7% 정산 API", description = "제휴 매장 등록, 메뉴 CRUD, 품절 토글, 예약 슬롯 개설, 7% 플랫폼 수수료 및 정산 조회 (4-8)")
@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    @Operation(summary = "신규 매장 등록 (관리자/점주, ADM-01)", description = "상호명, 주소, 위경도 좌표, 테마거리 매핑을 통해 신규 매장을 등록합니다.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreAdminResponse>> createStore(@Valid @RequestBody StoreCreateRequest request) {
        StoreAdminResponse response = adminStoreService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("매장이 성공적으로 등록되었습니다.", response));
    }

    @Operation(summary = "매장 상세 정보 조회 (관리자/점주, ADM-01)", description = "특정 매장의 기본 정보 및 등록된 메뉴 목록을 조회합니다.")
    @GetMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreAdminResponse>> getStore(@PathVariable Long storeId) {
        StoreAdminResponse response = adminStoreService.getStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "전체 매장 목록 페이징 조회 (관리자, ADM-01)", description = "플랫폼에 등록된 전체 매장 목록을 페이징으로 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<StoreAdminResponse>>> getAllStores(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StoreAdminResponse> responses = adminStoreService.getAllStores(pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "매장 정보 수정 (관리자/점주, ADM-01)", description = "영업시간, 연락처, 제휴 혜택 문구 등을 수정합니다.")
    @PatchMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreAdminResponse>> updateStore(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreUpdateRequest request) {
        StoreAdminResponse response = adminStoreService.updateStore(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "매장 제휴 승인/반려 (플랫폼 관리자 전용, ADM-01)", description = "계약 승인된 제휴 매장만 앱 지도 및 검색에 노출됩니다.")
    @PatchMapping("/{storeId}/partnership")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreAdminResponse>> updatePartnership(
            @PathVariable Long storeId,
            @Valid @RequestBody StorePartnershipRequest request) {
        StoreAdminResponse response = adminStoreService.updatePartnership(storeId, request.getIsPartner());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- 메뉴 관리 ---

    @Operation(summary = "디저트 메뉴 등록 (관리자/점주, ADM-01)", description = "매장에 새로운 대표 또는 일반 디저트 메뉴를 추가합니다.")
    @PostMapping("/{storeId}/menus")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreMenuResponse>> createMenu(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreMenuCreateRequest request) {
        StoreMenuResponse response = adminStoreService.createMenu(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("메뉴가 성공적으로 등록되었습니다.", response));
    }

    @Operation(summary = "디저트 메뉴 수정 (관리자/점주, ADM-01)", description = "메뉴명, 가격, 설명, 대표 메뉴 여부를 수정합니다.")
    @PatchMapping("/{storeId}/menus/{menuId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreMenuResponse>> updateMenu(
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @Valid @RequestBody StoreMenuUpdateRequest request) {
        StoreMenuResponse response = adminStoreService.updateMenu(storeId, menuId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "디저트 메뉴 삭제 (관리자/점주, ADM-01)", description = "등록된 디저트 메뉴를 삭제합니다.")
    @DeleteMapping("/{storeId}/menus/{menuId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        adminStoreService.deleteMenu(storeId, menuId);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 삭제되었습니다.", null));
    }

    @Operation(summary = "메뉴 품절 실시간 토글 (관리자/점주, ADM-01)", description = "당일 재료 소진에 따라 품절(isSoldOut) 상태를 원클릭 토글합니다.")
    @PatchMapping("/{storeId}/menus/{menuId}/sold-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreMenuResponse>> toggleMenuSoldOut(
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        StoreMenuResponse response = adminStoreService.toggleMenuSoldOut(storeId, menuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- 예약 슬롯 관리 ---

    @Operation(summary = "예약 슬롯 개설 (관리자/점주, ADM-02)", description = "날짜 및 시간대별 데이트 예약 가능 수용 인원(팀 수) 슬롯을 생성합니다.")
    @PostMapping("/{storeId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<ReservationSlotResponse>> createReservationSlot(
            @PathVariable Long storeId,
            @Valid @RequestBody ReservationSlotCreateRequest request) {
        ReservationSlotResponse response = adminStoreService.createReservationSlot(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("예약 슬롯이 성공적으로 개설되었습니다.", response));
    }

    @Operation(summary = "특정 일자 예약 슬롯 조회 (관리자/점주, ADM-02)", description = "지정된 날짜의 예약 가능 슬롯 및 잔여 현황을 확인합니다. 미지정 시 당일 기준 조회.")
    @GetMapping("/{storeId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<List<ReservationSlotResponse>>> getReservationSlots(
            @PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate) {
        LocalDate targetDate = (slotDate != null) ? slotDate : LocalDate.now();
        List<ReservationSlotResponse> responses = adminStoreService.getReservationSlots(storeId, targetDate);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // --- 수수료 및 정산 집계 ---

    @Operation(summary = "매장 7% 플랫폼 수수료 및 정산액 조회 (단디톤 P1, ADM-03)",
            description = "방문 완료된 주문 총액을 기준으로 7% 플랫폼 수수료와 93% 실수령 정산액을 실시간 동적 계산하여 반환합니다.")
    @GetMapping("/{storeId}/settlement")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreSettlementResponse>> getSettlement(@PathVariable Long storeId) {
        StoreSettlementResponse response = adminStoreService.getSettlement(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- 자율 할인쿠폰 관리 ---

    @Operation(summary = "매장 자율 할인쿠폰 등록 (점주/관리자, ADM-06)", description = "점주가 매장 데이트 예약 시 적용 가능한 할인쿠폰을 자율 등록합니다.")
    @PostMapping("/{storeId}/coupons")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreCouponResponse>> createCoupon(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreCouponCreateRequest request) {
        StoreCouponResponse response = adminStoreService.createCoupon(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("할인쿠폰이 등록되었습니다.", response));
    }

    @Operation(summary = "매장 등록 할인쿠폰 목록 조회 (점주/관리자, ADM-06)", description = "매장에 등록된 전체 할인쿠폰 목록(활성/비활성 포함)을 조회합니다.")
    @GetMapping("/{storeId}/coupons")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<List<StoreCouponResponse>>> getStoreCoupons(@PathVariable Long storeId) {
        List<StoreCouponResponse> responses = adminStoreService.getStoreCoupons(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "매장 할인쿠폰 활성화/비활성화 토글 (점주/관리자, ADM-06)", description = "쿠폰의 사용 가능(isActive) 상태를 토글합니다.")
    @PatchMapping("/{storeId}/coupons/{couponId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<StoreCouponResponse>> toggleCoupon(
            @PathVariable Long storeId,
            @PathVariable Long couponId) {
        StoreCouponResponse response = adminStoreService.toggleCoupon(storeId, couponId);
        return ResponseEntity.ok(ApiResponse.success("쿠폰 활성화 상태가 변경되었습니다.", response));
    }
}
