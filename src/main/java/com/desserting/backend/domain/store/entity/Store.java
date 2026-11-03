package com.desserting.backend.domain.store.entity;

import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_street_id")
    private ThemeStreet themeStreet;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(length = 100)
    private String businessHours;

    @Column(length = 30)
    private String contact;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPartner = true; // 제휴 매장 여부

    @Column(length = 255)
    private String partnerDiscountNotice; // 예: "디저팅 회원 전 메뉴 10% 할인"

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreMenu> menus = new ArrayList<>();

    public void updateInfo(String name, String address, BigDecimal latitude, BigDecimal longitude,
                           String businessHours, String contact, String coverImageUrl,
                           ThemeStreet themeStreet, String partnerDiscountNotice) {
        if (name != null && !name.isBlank()) this.name = name;
        if (address != null && !address.isBlank()) this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (businessHours != null) this.businessHours = businessHours;
        if (contact != null) this.contact = contact;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (themeStreet != null) this.themeStreet = themeStreet;
        if (partnerDiscountNotice != null) this.partnerDiscountNotice = partnerDiscountNotice;
    }

    public void updatePartnership(Boolean isPartner) {
        if (isPartner != null) {
            this.isPartner = isPartner;
        }
    }

    public void updateDiscountNotice(String partnerDiscountNotice) {
        this.partnerDiscountNotice = partnerDiscountNotice;
    }
}
