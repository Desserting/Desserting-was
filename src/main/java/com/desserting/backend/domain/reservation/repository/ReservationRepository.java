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

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.store ORDER BY r.reservationDate DESC, r.reservationTime DESC, r.id DESC")
    List<Reservation> findAllWithUserAndStore();

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.store WHERE r.store.id = :storeId ORDER BY r.reservationDate DESC, r.reservationTime DESC, r.id DESC")
    List<Reservation> findAllWithUserAndStoreByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.store WHERE r.status = :status ORDER BY r.reservationDate DESC, r.reservationTime DESC, r.id DESC")
    List<Reservation> findAllWithUserAndStoreByStatus(@Param("status") ReservationStatus status);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.store WHERE r.store.id = :storeId AND r.status = :status ORDER BY r.reservationDate DESC, r.reservationTime DESC, r.id DESC")
    List<Reservation> findAllWithUserAndStoreByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") ReservationStatus status);

    /** 쿠폰 할인 후 실결제 금액 합계 (메뉴 합계 - 할인, 음수는 0으로). 할인 없는 예약은 메뉴 합계 그대로. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN r.totalAmount > r.discountAmount THEN r.totalAmount - r.discountAmount ELSE 0 END), 0)
            FROM Reservation r WHERE r.store.id = :storeId AND r.status = :status
            """)
    Long sumPaidAmountByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") ReservationStatus status);
}
