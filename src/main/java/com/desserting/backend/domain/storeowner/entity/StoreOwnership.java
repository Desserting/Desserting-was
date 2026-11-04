package com.desserting.backend.domain.storeowner.entity;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 점주(STORE_OWNER)와 관리 매장의 연결 (SELL-01~09 권한 범위).
 * Store/User 엔티티(4-8, 4-1)를 수정하지 않기 위해 별도 테이블로 관리한다.
 */
@Entity
@Table(name = "store_owners",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_owner", columnNames = {"store_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreOwnership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static StoreOwnership of(Store store, User user) {
        return StoreOwnership.builder().store(store).user(user).build();
    }
}
