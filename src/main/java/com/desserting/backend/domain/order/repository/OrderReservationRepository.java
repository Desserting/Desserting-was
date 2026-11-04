package com.desserting.backend.domain.order.repository;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 4-7 주문용 예약 조회·금액 반영. 공용 Reservation 엔티티/ReservationRepository 는 수정하지 않는다.
 */
public interface OrderReservationRepository extends Repository<Reservation, Long> {

    /** 같은 예약에 동시에 두 번 결제되지 않도록 예약 행을 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r JOIN FETCH r.store WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.store WHERE r.id = :id")
    Optional<Reservation> findByIdWithStore(@Param("id") Long id);

    /** 결제 완료 금액을 예약에 반영 → 점주 예약 목록·정산(4-8)에서 그대로 보인다 */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.totalAmount = :totalAmount, r.discountAmount = :discountAmount,
                r.finalAmount = :finalAmount, r.coupon = :coupon
            WHERE r.id = :id
            """)
    int applyOrderAmounts(@Param("id") Long id,
                          @Param("totalAmount") int totalAmount,
                          @Param("discountAmount") int discountAmount,
                          @Param("finalAmount") int finalAmount,
                          @Param("coupon") StoreCoupon coupon);
}
