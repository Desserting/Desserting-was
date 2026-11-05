package com.desserting.backend.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "예약 및 결제 내역 채팅방 공유 요청 DTO")
public class ShareReservationRequest {

    @Schema(description = "공유할 예약 ID (RSV-03/ORD-01 완료된 예약)", example = "15")
    @NotNull(message = "예약 ID(reservationId)는 필수입니다.")
    private Long reservationId;

    @Schema(description = "함께 보낼 메시지 (선택)", example = "예약과 주문 완료했어요! 이따 만나요 :)")
    private String message;
}
