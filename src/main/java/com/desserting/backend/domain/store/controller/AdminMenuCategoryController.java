package com.desserting.backend.domain.store.controller;

import com.desserting.backend.domain.store.dto.MenuCategoryRequest;
import com.desserting.backend.domain.store.dto.MenuCategoryResponse;
import com.desserting.backend.domain.store.service.MenuCategoryService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 매장 상세 메뉴 탭(디저트/음료/세트) 분류 지정. 메뉴 등록·수정 API(4-8)는 그대로 두고 분류만 따로 관리한다. */
@Tag(name = "4-6. 제휴매장 검색 및 상세 API")
@RestController
@RequestMapping("/api/v1/admin/stores/{storeId}/menus/{menuId}/category")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminMenuCategoryController {

    private final MenuCategoryService menuCategoryService;

    @Operation(summary = "메뉴 분류 지정 (점주/관리자)",
            description = "매장 상세 메뉴 탭에 쓰이는 분류(DESSERT/DRINK/SET)를 지정합니다. "
                    + "지정하지 않은 메뉴는 이름으로 추정합니다(커피·라떼·에이드 등 → DRINK, '세트' → SET, 그 외 DESSERT).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A002 점주·관리자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "EX501 해당 매장의 메뉴가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ApiResponse<MenuCategoryResponse> assignCategory(@PathVariable Long storeId, @PathVariable Long menuId,
                                                            @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.success("메뉴 분류를 지정하였습니다.",
                MenuCategoryResponse.of(menuId, menuCategoryService.assign(storeId, menuId, request.category())));
    }
}
