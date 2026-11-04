package com.desserting.backend.domain.order.dto;

import com.desserting.backend.domain.order.entity.OrderProgressStep;
import com.desserting.backend.domain.order.entity.OrderStatus;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "주문 진행 단계 (ORD-03)")
public record OrderProgressStepResponse(
        @Schema(example = "PREPARING") OrderProgressStep step,
        @Schema(example = "준비 중") String label,
        @Schema(description = "이 단계에 도달했는지") boolean done,
        @Schema(description = "도달 시각. 점주가 예약 상태 API(4-8)로 바꿔 시각 기록이 없으면 done=true 여도 null") LocalDateTime at
) {

    /** 접수 → 준비 중 → 준비 완료. 예약 상태 API(4-8)로 PREPARING/COMPLETED 로 바뀐 경우도 단계에 반영한다. */
    public static List<OrderProgressStepResponse> of(ReservationOrder order) {
        ReservationStatus rs = order.getReservation().getStatus();
        boolean preparing = order.getPreparingAt() != null
                || rs == ReservationStatus.PREPARING || rs == ReservationStatus.COMPLETED;
        boolean ready = order.getReadyAt() != null || rs == ReservationStatus.COMPLETED;
        return List.of(
                new OrderProgressStepResponse(OrderProgressStep.RECEIVED, OrderProgressStep.RECEIVED.getLabel(),
                        true, order.getPaidAt()),
                new OrderProgressStepResponse(OrderProgressStep.PREPARING, OrderProgressStep.PREPARING.getLabel(),
                        preparing, order.getPreparingAt()),
                new OrderProgressStepResponse(OrderProgressStep.READY, OrderProgressStep.READY.getLabel(),
                        ready, order.getReadyAt()));
    }

    /** 가장 마지막으로 도달한 단계. 환불된 주문은 null */
    public static OrderProgressStep current(ReservationOrder order, List<OrderProgressStepResponse> steps) {
        if (order.getStatus() != OrderStatus.PAID) {
            return null;
        }
        OrderProgressStep current = OrderProgressStep.RECEIVED;
        for (OrderProgressStepResponse s : steps) {
            if (s.done()) {
                current = s.step();
            }
        }
        return current;
    }
}
