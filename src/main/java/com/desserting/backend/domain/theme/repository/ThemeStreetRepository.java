package com.desserting.backend.domain.theme.repository;

import com.desserting.backend.domain.theme.entity.ThemeStreet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeStreetRepository extends JpaRepository<ThemeStreet, Long> {
    List<ThemeStreet> findAllByIsActiveTrue();
}
