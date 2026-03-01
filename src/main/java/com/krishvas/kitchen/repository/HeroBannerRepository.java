package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long> {
    List<HeroBanner> findByActiveTrueOrderByPositionOrderAsc();
}
