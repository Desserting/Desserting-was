package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.ThemeStreetCreateRequest;
import com.desserting.backend.domain.admin.dto.ThemeStreetResponse;
import com.desserting.backend.domain.admin.dto.ThemeStreetUpdateRequest;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.repository.StoreRepository;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminThemeService {

    private final ThemeStreetRepository themeStreetRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public ThemeStreetResponse createThemeStreet(ThemeStreetCreateRequest request) {
        ThemeStreet themeStreet = ThemeStreet.builder()
                .name(request.getName())
                .description(request.getDescription())
                .boundaryPolygonJson(request.getBoundaryPolygonJson())
                .centerLatitude(request.getCenterLatitude())
                .centerLongitude(request.getCenterLongitude())
                .coverImageUrl(request.getCoverImageUrl())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();

        ThemeStreet saved = themeStreetRepository.save(themeStreet);
        log.info("테마거리 등록 완료: themeId={}, name={}", saved.getId(), saved.getName());
        return ThemeStreetResponse.from(saved, List.of());
    }

    @Transactional(readOnly = true)
    public ThemeStreetResponse getThemeStreet(Long themeId) {
        ThemeStreet themeStreet = themeStreetRepository.findById(themeId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));
        List<Store> stores = storeRepository.findAllByThemeStreetIdAndIsPartnerTrue(themeId);
        return ThemeStreetResponse.from(themeStreet, stores);
    }

    @Transactional(readOnly = true)
    public List<ThemeStreetResponse> getAllThemeStreets() {
        return themeStreetRepository.findAll().stream()
                .map(theme -> {
                    List<Store> stores = storeRepository.findAllByThemeStreetIdAndIsPartnerTrue(theme.getId());
                    return ThemeStreetResponse.from(theme, stores);
                })
                .toList();
    }

    @Transactional
    public ThemeStreetResponse updateThemeStreet(Long themeId, ThemeStreetUpdateRequest request) {
        ThemeStreet themeStreet = themeStreetRepository.findById(themeId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));

        themeStreet.updateTheme(
                request.getName(),
                request.getDescription(),
                request.getBoundaryPolygonJson(),
                request.getCenterLatitude(),
                request.getCenterLongitude(),
                request.getCoverImageUrl(),
                request.getIsActive()
        );

        log.info("테마거리 수정 완료: themeId={}, name={}", themeStreet.getId(), themeStreet.getName());
        List<Store> stores = storeRepository.findAllByThemeStreetIdAndIsPartnerTrue(themeId);
        return ThemeStreetResponse.from(themeStreet, stores);
    }

    @Transactional
    public ThemeStreetResponse assignStoreToThemeStreet(Long themeId, Long storeId) {
        ThemeStreet themeStreet = themeStreetRepository.findById(themeId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        store.assignThemeStreet(themeStreet);
        log.info("테마거리에 매장 추가 완료: themeId={}, storeId={}, storeName={}", themeId, storeId, store.getName());
        List<Store> stores = storeRepository.findAllByThemeStreetIdAndIsPartnerTrue(themeId);
        return ThemeStreetResponse.from(themeStreet, stores);
    }

    @Transactional
    public ThemeStreetResponse removeStoreFromThemeStreet(Long themeId, Long storeId) {
        ThemeStreet themeStreet = themeStreetRepository.findById(themeId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_STREET_NOT_FOUND));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        if (store.getThemeStreet() != null && store.getThemeStreet().getId().equals(themeId)) {
            store.removeThemeStreet();
            log.info("테마거리에서 매장 제거 완료: themeId={}, storeId={}, storeName={}", themeId, storeId, store.getName());
        }
        List<Store> stores = storeRepository.findAllByThemeStreetIdAndIsPartnerTrue(themeId);
        return ThemeStreetResponse.from(themeStreet, stores);
    }
}
