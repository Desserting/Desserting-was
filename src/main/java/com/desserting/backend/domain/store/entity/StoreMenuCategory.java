package com.desserting.backend.domain.store.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 점주가 직접 지정한 메뉴 분류. 지정하지 않은 메뉴는 {@link MenuCategory#infer(String)} 로 추정한다.
 * StoreMenu 엔티티(4-8)를 수정하지 않기 위해 별도 테이블로 관리한다.
 */
@Entity
@Table(name = "store_menu_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_menu_category_menu", columnNames = "menu_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreMenuCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private StoreMenu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuCategory category;

    public static StoreMenuCategory of(StoreMenu menu, MenuCategory category) {
        return StoreMenuCategory.builder().menu(menu).category(category).build();
    }

    public void change(MenuCategory category) {
        this.category = category;
    }
}
