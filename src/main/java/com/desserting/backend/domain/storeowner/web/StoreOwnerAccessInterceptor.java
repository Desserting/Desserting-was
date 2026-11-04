package com.desserting.backend.domain.storeowner.web;

import com.desserting.backend.domain.storeowner.repository.StoreOwnershipRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /api/v1/admin/stores/{storeId}/** 에 점주(STORE_OWNER)가 접근하면 자기 매장인지 확인한다.
 * 관리자(ADMIN)는 모든 매장에 접근할 수 있다. 기존 4-8 컨트롤러·서비스는 수정하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOwnerAccessInterceptor implements HandlerInterceptor {

    private final StoreOwnershipRepository ownershipRepository;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return true; // 인증·역할 검사는 Spring Security / @PreAuthorize 가 담당
        }
        Set<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN") || !roles.contains("ROLE_STORE_OWNER")) {
            return true;
        }

        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String raw = vars == null ? null : vars.get("storeId");
        if (raw == null) {
            return true;
        }
        Long storeId;
        try {
            storeId = Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return true; // 숫자가 아니면 컨트롤러의 타입 변환 오류로 처리
        }

        if (!ownershipRepository.existsByStoreIdAndUserId(storeId, user.getUserId())) {
            log.warn("점주 매장 접근 거부: userId={}, storeId={}, uri={}", user.getUserId(), storeId, request.getRequestURI());
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인이 관리하는 매장만 접근할 수 있습니다.");
        }
        return true;
    }
}
