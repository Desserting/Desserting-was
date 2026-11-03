package com.desserting.backend.domain.reservation.repository;

import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 예약(4-6) 전용 리포지토리. 공용 ReservationRepository 는 수정하지 않는다.
 */
public interface UserReservationRepository extends Repository<Reservation, Long> {

    Reservation save(Reservation reservation);

    @Query("""
            SELECT r FROM Reservation r JOIN FETCH r.store
            WHERE r.user.id = :userId
            ORDER BY r.reservationDate DESC, r.reservationTime DESC
            """)
    List<Reservation> findMyReservations(@Param("userId") Long userId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.store WHERE r.id = :id")
    Optional<Reservation> findByIdWithStore(@Param("id") Long id);

    boolean existsByUserIdAndStoreIdAndReservationDateAndReservationTimeAndStatusNot(
            Long userId, Long storeId, LocalDate reservationDate, LocalTime reservationTime, ReservationStatus status);
}
