package com.desserting.backend.domain.storeowner.web;

import com.desserting.backend.domain.storeowner.repository.StoreOwnershipRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreOwnerAccessInterceptorTest {

    @Mock private StoreOwnershipRepository ownershipRepository;
    @InjectMocks private StoreOwnerAccessInterceptor interceptor;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("점주는 자기 매장이면 통과, 아니면 A002")
    void ownerScope() {
        login(UserRole.ROLE_STORE_OWNER, 7L);
        given(ownershipRepository.existsByStoreIdAndUserId(1L, 7L)).willReturn(true);
        given(ownershipRepository.existsByStoreIdAndUserId(2L, 7L)).willReturn(false);

        assertThat(interceptor.preHandle(request("1"), new MockHttpServletResponse(), null)).isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(request("2"), new MockHttpServletResponse(), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("관리자와 일반 경로(storeId 없음)는 검사하지 않는다")
    void adminAndNoStoreId() {
        login(UserRole.ROLE_ADMIN, 1L);
        assertThat(interceptor.preHandle(request("2"), new MockHttpServletResponse(), null)).isTrue();

        login(UserRole.ROLE_STORE_OWNER, 7L);
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null)).isTrue();
        verify(ownershipRepository, never()).existsByStoreIdAndUserId(anyLong(), anyLong());
    }

    private void login(UserRole role, Long id) {
        User u = User.builder().email("u@d.com").role(role).build();
        ReflectionTestUtils.setField(u, "id", id);
        CustomUserDetails d = CustomUserDetails.create(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities()));
    }

    private MockHttpServletRequest request(String storeId) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("storeId", storeId));
        return r;
    }
}
