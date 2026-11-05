package com.desserting.backend.domain.storeowner.repository;

import com.desserting.backend.domain.storeowner.entity.StoreOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreOwnershipRepository extends JpaRepository<StoreOwnership, Long> {

    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    @Query("SELECT o FROM StoreOwnership o JOIN FETCH o.store WHERE o.user.id = :userId ORDER BY o.store.id")
    List<StoreOwnership> findAllByUserIdWithStore(@Param("userId") Long userId);
}
