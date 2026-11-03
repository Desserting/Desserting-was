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

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r WHERE r.store.id = :storeId AND r.status = :status")
    Long sumTotalAmountByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") ReservationStatus status);
}
