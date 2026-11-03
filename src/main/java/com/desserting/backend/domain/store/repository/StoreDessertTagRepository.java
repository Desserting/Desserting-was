package com.desserting.backend.domain.store.repository;

import com.desserting.backend.domain.store.entity.StoreDessertTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StoreDessertTagRepository extends JpaRepository<StoreDessertTag, Long> {

    @Query("SELECT t FROM StoreDessertTag t WHERE t.store.id IN :storeIds ORDER BY t.id")
    List<StoreDessertTag> findAllByStoreIds(@Param("storeIds") Collection<Long> storeIds);

    boolean existsByStoreId(Long storeId);
}
