package com.desserting.backend.domain.order.repository;

import com.desserting.backend.domain.order.entity.ReservationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationOrderRepository extends JpaRepository<ReservationOrder, Long> {

    boolean existsByReservationId(Long reservationId);

    @Query("""
            SELECT DISTINCT o FROM ReservationOrder o
            JOIN FETCH o.reservation JOIN FETCH o.store LEFT JOIN FETCH o.items
            WHERE o.reservation.id = :reservationId
            """)
    Optional<ReservationOrder> findDetailByReservationId(@Param("reservationId") Long reservationId);

    @Query("""
            SELECT DISTINCT o FROM ReservationOrder o
            JOIN FETCH o.reservation JOIN FETCH o.store LEFT JOIN FETCH o.items
            WHERE o.user.id = :userId
            ORDER BY o.id DESC
            """)
    List<ReservationOrder> findAllDetailByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT o FROM ReservationOrder o
            JOIN FETCH o.reservation r JOIN FETCH r.user JOIN FETCH o.store LEFT JOIN FETCH o.items
            WHERE o.store.id = :storeId
            ORDER BY o.id DESC
            """)
    List<ReservationOrder> findAllDetailByStoreId(@Param("storeId") Long storeId);
}
