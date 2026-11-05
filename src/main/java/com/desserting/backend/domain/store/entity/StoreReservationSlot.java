package com.desserting.backend.domain.store.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "store_reservation_slots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_slot_date_time", columnNames = {"store_id", "slot_date", "slot_time"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreReservationSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false)
    private LocalTime slotTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxCapacity = 4; // 해당 시간대 수용 가능 테이블/팀 수

    @Column(nullable = false)
    @Builder.Default
    private Integer currentReservedCount = 0; // 현재 확정된 예약 건수

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true; // 예약 가능 여부 (정원 도달 시 false)

    @Version
    private Long version;

    public void updateCapacity(Integer maxCapacity) {
        if (maxCapacity != null && maxCapacity > 0) {
            this.maxCapacity = maxCapacity;
            this.isAvailable = this.currentReservedCount < this.maxCapacity;
        }
    }

    public void increaseReservation() {
        if (this.currentReservedCount >= this.maxCapacity) {
            throw new CustomException(ErrorCode.RESERVATION_SLOT_FULL);
        }
        this.currentReservedCount++;
        if (this.currentReservedCount >= this.maxCapacity) {
            this.isAvailable = false;
        }
    }

    public void decreaseReservation() {
        if (this.currentReservedCount > 0) {
            this.currentReservedCount--;
        }
        if (this.currentReservedCount < this.maxCapacity) {
            this.isAvailable = true;
        }
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
}
