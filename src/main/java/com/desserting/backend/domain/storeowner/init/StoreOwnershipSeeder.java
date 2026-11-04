package com.desserting.backend.domain.storeowner.init;

import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.storeowner.entity.StoreOwnership;
import com.desserting.backend.domain.storeowner.repository.StoreOwnershipRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 시드 점주 계정(AdminDataInitializer)을 시드 매장에 연결한다. 이미 연결돼 있으면 건너뛴다.
 * 매장은 이름 앞부분으로 찾는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOwnershipSeeder {

    private static final Map<String, String> OWNER_EMAIL_TO_STORE_PREFIX = Map.of(
            "owner@anook.com", "아눅 앞산",
            "owner@lucid.com", "카페 루시드");

    private final StoreOwnershipRepository ownershipRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        OWNER_EMAIL_TO_STORE_PREFIX.forEach((email, prefix) -> userRepository.findByEmail(email).ifPresent(owner ->
                storeRepository.findAll().stream()
                        .filter(s -> s.getName() != null && s.getName().startsWith(prefix))
                        .forEach(store -> {
                            if (!ownershipRepository.existsByStoreIdAndUserId(store.getId(), owner.getId())) {
                                ownershipRepository.save(StoreOwnership.of(store, owner));
                                log.info("시드 점주 매장 연결: {} -> {}", email, store.getName());
                            }
                        })));
    }
}
