package com.desserting.backend.domain.storeowner.controller;

import com.desserting.backend.domain.storeowner.dto.OwnedStoreResponse;
import com.desserting.backend.domain.storeowner.repository.StoreOwnershipRepository;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4-8. 점주 매장 범위")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StoreOwnerController {

    private final StoreOwnershipRepository ownershipRepository;

    @Operation(summary = "내 관리 매장 목록 (점주)",
            description = "점주는 이 목록의 매장에 대해서만 /api/v1/admin/stores/{storeId}/** API 를 쓸 수 있습니다. 다른 매장은 A002.")
    @GetMapping("/api/v1/admin/my-stores")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    @Transactional(readOnly = true)
    public ApiResponse<List<OwnedStoreResponse>> getMyStores(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("관리 매장 목록 조회에 성공하였습니다.",
                ownershipRepository.findAllByUserIdWithStore(userId).stream()
                        .map(o -> OwnedStoreResponse.from(o.getStore())).toList());
    }
}
