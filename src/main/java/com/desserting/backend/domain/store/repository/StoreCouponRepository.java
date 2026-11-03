package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreCouponRepository extends JpaRepository<StoreCoupon, Long> {

    List<StoreCoupon> findAllByStoreIdAndIsActiveTrueOrderByIdDesc(Long storeId);

    List<StoreCoupon> findAllByStoreIdOrderByIdDesc(Long storeId);

    Optional<StoreCoupon> findByIdAndStoreId(Long id, Long storeId);
}
