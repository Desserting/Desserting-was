package com.desserting.backend.domain.storeowner.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 공용 WebMvcConfig 는 그대로 두고 점주 매장 범위 검사만 추가로 등록한다 */
@Configuration
@RequiredArgsConstructor
public class StoreOwnerWebConfig implements WebMvcConfigurer {

    private final StoreOwnerAccessInterceptor storeOwnerAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storeOwnerAccessInterceptor)
                .addPathPatterns("/api/v1/admin/stores/*", "/api/v1/admin/stores/*/**");
    }
}
