package com.desserting.backend.domain.store.controller;

import com.desserting.backend.domain.store.dto.StoreCardResponse;
import com.desserting.backend.domain.store.dto.StoreDetailResponse;
import com.desserting.backend.domain.store.dto.StoreSearchCondition;
import com.desserting.backend.domain.store.service.StoreQueryService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.ErrorResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4-6. 제휴매장 탐색 API", description = "제휴매장 지도 마커·목록 검색 및 매장 상세 API (4-5, 4-6)")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreQueryService storeQueryService;

    @Operation(summary = "제휴매장 검색 (MAP-04~06, SHOP-01)",
            description = """
                    제휴 계약 매장만 반환합니다. 모든 조건은 선택값이며 함께 적용됩니다.
                    - themeStreetId: 테마거리 구역 내 매장
                    - dessertType: 해당 디저트를 판매하는 매장
                    - region: 주소 접두어 (예: 대구 남구)
                    - swLat/swLng/neLat/neLng: 현재 지도 화면 영역 (네 값 모두 필요)
                    - lat/lng: 거리 계산 기준 (없으면 내 저장 위치). 기준이 있으면 가까운 순, 없으면 이름순.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공 (결과가 없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EX201 지도 영역 값 일부 누락·역전 / EX001 파라미터 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ApiResponse<List<StoreCardResponse>> searchStores(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @ParameterObject @ModelAttribute StoreSearchCondition condition) {
        return ApiResponse.success("제휴매장 검색에 성공하였습니다.", storeQueryService.searchStores(userId, condition));
    }

    @Operation(summary = "제휴매장 상세 (SHOP-02, SHOP-03, MAP-08)",
            description = "위치·영업시간·연락처·제휴 할인 안내·판매 디저트·메뉴 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "S001 매장 없음 또는 제휴 해지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{storeId}")
    public ApiResponse<StoreDetailResponse> getStoreDetail(@PathVariable Long storeId) {
        return ApiResponse.success("매장 상세 조회에 성공하였습니다.", storeQueryService.getStoreDetail(storeId));
    }
}
