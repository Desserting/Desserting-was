package com.desserting.backend.domain.storeowner.web;

import com.desserting.backend.domain.admin.controller.AdminStoreController;
import com.desserting.backend.domain.admin.dto.StoreAdminResponse;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.storeowner.entity.StoreOwnership;
import com.desserting.backend.domain.storeowner.repository.StoreOwnershipRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 점주가 매장을 등록(POST /api/v1/admin/stores)하면 그 매장의 점주로 연결한다.
 * AdminStoreController/AdminStoreService(4-8)는 수정하지 않고 응답의 storeId 를 이용한다.
 */
@Slf4j
@ControllerAdvice(assignableTypes = AdminStoreController.class)
@RequiredArgsConstructor
public class StoreCreatedOwnershipAdvice implements ResponseBodyAdvice<Object> {

    private final StoreOwnershipRepository ownershipRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethod() != null && "createStore".equals(returnType.getMethod().getName());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> api && api.getData() instanceof StoreAdminResponse store) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean owner = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_STORE_OWNER".equals(a.getAuthority()));
            if (owner && auth.getPrincipal() instanceof CustomUserDetails user && store.getStoreId() != null
                    && !ownershipRepository.existsByStoreIdAndUserId(store.getStoreId(), user.getUserId())) {
                ownershipRepository.save(StoreOwnership.of(
                        storeRepository.getReferenceById(store.getStoreId()),
                        userRepository.getReferenceById(user.getUserId())));
                log.info("점주 매장 등록 연결: userId={}, storeId={}", user.getUserId(), store.getStoreId());
            }
        }
        return body;
    }
}
