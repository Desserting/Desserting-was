package com.desserting.backend.domain.reservation.repository;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByUserIdOrderByReservationDateDesc(Long userId);
    List<Reservation> findAllByStoreIdOrderByReservationDateDesc(Long storeId);
    List<Reservation> findAllByStoreIdAndStatus(Long storeId, ReservationStatus status);

    long countByStoreIdAndStatus(Long storeId, ReservationStatus status);

    /** 쿠폰 할인 후 실결제 금액 합계 (메뉴 합계 - 할인, 음수는 0으로). 할인 없는 예약은 메뉴 합계 그대로. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN r.totalAmount > r.discountAmount THEN r.totalAmount - r.discountAmount ELSE 0 END), 0)
            FROM Reservation r WHERE r.store.id = :storeId AND r.status = :status
            """)
    Long sumPaidAmountByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") ReservationStatus status);
}
