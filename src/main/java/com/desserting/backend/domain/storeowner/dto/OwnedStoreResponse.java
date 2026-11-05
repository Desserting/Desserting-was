package com.desserting.backend.domain.storeowner.dto;

import com.desserting.backend.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 관리하는 매장")
public record OwnedStoreResponse(
        @Schema(example = "1") Long storeId,
        @Schema(example = "아눅 앞산 (a.nook Apsan)") String name,
        String address,
        boolean partner
) {
    public static OwnedStoreResponse from(Store s) {
        return new OwnedStoreResponse(s.getId(), s.getName(), s.getAddress(), Boolean.TRUE.equals(s.getIsPartner()));
    }
}
