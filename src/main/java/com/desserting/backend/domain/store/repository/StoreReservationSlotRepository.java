package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreReservationSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface StoreReservationSlotRepository extends JpaRepository<StoreReservationSlot, Long> {
    List<StoreReservationSlot> findAllByStoreIdAndSlotDateOrderBySlotTimeAsc(Long storeId, LocalDate slotDate);
    boolean existsByStoreIdAndSlotDateAndSlotTime(Long storeId, LocalDate slotDate, LocalTime slotTime);
    Optional<StoreReservationSlot> findByStoreIdAndSlotDateAndSlotTime(Long storeId, LocalDate slotDate, LocalTime slotTime);
}
