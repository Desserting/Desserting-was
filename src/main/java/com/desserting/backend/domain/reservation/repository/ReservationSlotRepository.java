package com.desserting.backend.domain.reservation.repository;

import com.desserting.backend.domain.reservation.entity.ReservationSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {

    List<ReservationSlot> findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(Long storeId, LocalDate slotDate);

    List<ReservationSlot> findAllByStoreIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            Long storeId, LocalDate from, LocalDate to);

    @Query("SELECT s FROM ReservationSlot s JOIN FETCH s.store WHERE s.id = :id")
    Optional<ReservationSlot> findByIdWithStore(@Param("id") Long id);

    boolean existsByStoreIdAndSlotDate(Long storeId, LocalDate slotDate);

    /**
     * 정원 안에서만 원자적으로 인원을 더한다. 반영된 행 수가 0이면 정원 초과.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ReservationSlot s SET s.reservedCount = s.reservedCount + :guests
            WHERE s.id = :id AND s.reservedCount + :guests <= s.capacity
            """)
    int tryReserve(@Param("id") Long id, @Param("guests") int guests);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ReservationSlot s SET s.reservedCount = s.reservedCount - :guests
            WHERE s.id = :id AND s.reservedCount >= :guests
            """)
    int release(@Param("id") Long id, @Param("guests") int guests);
}
