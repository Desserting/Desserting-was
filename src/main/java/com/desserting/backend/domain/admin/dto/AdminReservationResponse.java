package com.desserting.backend.domain.admin.dto;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 점주/관리자용 예약 및 주문 상세 응답 DTO")
public class AdminReservationResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long reservationId;

    @Schema(description = "예약자 유저 ID", example = "5")
    private Long userId;

    @Schema(description = "예약자 닉네임", example = "딸기휘낭시에")
    private String userNickname;

    @Schema(description = "예약 매장 ID", example = "1")
    private Long storeId;

    @Schema(description = "예약 매장명", example = "카페 루시드")
    private String storeName;

    @Schema(description = "방문 예약 일자", example = "2026-11-10")
    private LocalDate reservationDate;

    @Schema(description = "방문 예약 시간", example = "15:00:00")
    private LocalTime reservationTime;

    @Schema(description = "방문 인원", example = "2")
    private Integer guestCount;

    @Schema(description = "예약 진행 상태 (PENDING, CONFIRMED, PREPARING, COMPLETED, CANCELLED)", example = "PREPARING")
    private ReservationStatus status;

    @Schema(description = "메뉴 선주문 총 금액", example = "25500")
    private Integer totalAmount;

    @Schema(description = "고객 요청 사항", example = "조용한 창가 자리로 부탁드려요")
    private String specialRequest;

    @Schema(description = "예약 접수 일시")
    private LocalDateTime createdAt;

    public static AdminReservationResponse from(Reservation reservation) {
        return AdminReservationResponse.builder()
                .reservationId(reservation.getId())
                .userId(reservation.getUser().getId())
                .userNickname(reservation.getUser().getNickname())
                .storeId(reservation.getStore().getId())
                .storeName(reservation.getStore().getName())
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .guestCount(reservation.getGuestCount())
                .status(reservation.getStatus())
                .totalAmount(reservation.getTotalAmount())
                .specialRequest(reservation.getSpecialRequest())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
