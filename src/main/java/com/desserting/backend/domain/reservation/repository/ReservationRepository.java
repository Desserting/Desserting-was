package com.desserting.backend.domain.reservation.repository;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByUserIdOrderByReservationDateDesc(Long userId);
    List<Reservation> findAllByStoreIdOrderByReservationDateDesc(Long storeId);

    @Query("""
            SELECT r FROM Reservation r JOIN FETCH r.store
            WHERE r.user.id = :userId
            ORDER BY r.reservationDate DESC, r.reservationTime DESC
            """)
    List<Reservation> findMyReservations(@Param("userId") Long userId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.store WHERE r.id = :id")
    Optional<Reservation> findByIdWithStore(@Param("id") Long id);

    boolean existsByUserIdAndSlotIdAndStatusNot(Long userId, Long slotId, ReservationStatus status);
}
