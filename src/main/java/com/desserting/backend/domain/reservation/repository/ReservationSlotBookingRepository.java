package com.desserting.backend.domain.reservation.repository;

import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 예약(4-6)용 슬롯 조회·차감 리포지토리.
 * StoreReservationSlot 엔티티/리포지토리(4-8)는 수정하지 않고, 정원 규칙(예약 1건 = 1팀)을 그대로 따른다.
 */
public interface ReservationSlotBookingRepository extends Repository<StoreReservationSlot, Long> {

    List<StoreReservationSlot> findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(Long storeId, LocalDate slotDate);

    List<StoreReservationSlot> findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            Long storeId, LocalDate from, LocalDate to);

    @Query("SELECT s FROM StoreReservationSlot s JOIN FETCH s.store WHERE s.id = :id")
    Optional<StoreReservationSlot> findByIdWithStore(@Param("id") Long id);

    Optional<StoreReservationSlot> findByStoreIdAndSlotDateAndSlotTime(Long storeId, LocalDate slotDate, LocalTime slotTime);

    /**
     * 예약 1건(1팀)을 원자적으로 추가한다. 마감·판매중지 슬롯이면 0 반환.
     * 동시 요청에서도 정원을 넘지 않도록 조건부 UPDATE 한 번으로 처리한다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE StoreReservationSlot s
            SET s.currentReservedCount = s.currentReservedCount + 1,
                s.isAvailable = CASE WHEN s.currentReservedCount + 1 >= s.maxCapacity THEN false ELSE true END
            WHERE s.id = :id AND s.isAvailable = true AND s.currentReservedCount < s.maxCapacity
            """)
    int tryReserve(@Param("id") Long id);

    /** 예약 1건 취소 시 자리 반환 (StoreReservationSlot.decreaseReservation 과 같은 규칙) */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE StoreReservationSlot s
            SET s.currentReservedCount = s.currentReservedCount - 1, s.isAvailable = true
            WHERE s.id = :id AND s.currentReservedCount > 0
            """)
    int release(@Param("id") Long id);
}
