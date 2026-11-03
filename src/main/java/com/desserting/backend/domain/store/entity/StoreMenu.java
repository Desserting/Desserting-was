package com.desserting.backend.domain.store.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreMenu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(length = 255)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRepresentative = false; // 대표 디저트 메뉴 여부

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSoldOut = false;

    public void updateMenu(String name, Integer price, String description, String imageUrl, Boolean isRepresentative) {
        if (name != null && !name.isBlank()) this.name = name;
        if (price != null && price >= 0) this.price = price;
        if (description != null) this.description = description;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (isRepresentative != null) this.isRepresentative = isRepresentative;
    }

    public boolean toggleSoldOut() {
        this.isSoldOut = !Boolean.TRUE.equals(this.isSoldOut);
        return this.isSoldOut;
    }

    public void setSoldOut(boolean isSoldOut) {
        this.isSoldOut = isSoldOut;
    }
}
