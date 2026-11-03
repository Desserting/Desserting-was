package com.desserting.backend.domain.reservation.entity;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 매장별 예약 가능 시간대 (RSV-01/02). 정원(capacity)은 인원 수 기준.
 * 생성·수정은 매장 관리자 기능(SELL-04)에서 담당하고, 4-6에서는 조회·차감만 한다.
 */
@Entity
@Table(name = "reservation_slots",
        uniqueConstraints = @UniqueConstraint(name = "uk_slot_store_date_time",
                columnNames = {"store_id", "slot_date", "slot_time"}),
        indexes = @Index(name = "idx_slot_store_date", columnList = "store_id, slot_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReservationSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedCount = 0;

    public int remaining() {
        return Math.max(0, capacity - reservedCount);
    }
}
