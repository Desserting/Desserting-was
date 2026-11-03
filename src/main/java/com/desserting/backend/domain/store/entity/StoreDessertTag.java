package com.desserting.backend.domain.store.entity;

import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 매장이 판매하는 디저트 종류 태그 (지도·매장 디저트 필터 MAP-06, SHOP-01).
 * Store/StoreMenu 엔티티를 수정하지 않기 위해 별도 테이블로 관리한다.
 */
@Entity
@Table(name = "store_dessert_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_dessert_tag", columnNames = {"store_id", "dessert_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreDessertTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "dessert_type", nullable = false, length = 50)
    private DessertType dessertType;

    public static StoreDessertTag of(Store store, DessertType dessertType) {
        return StoreDessertTag.builder().store(store).dessertType(dessertType).build();
    }
}
