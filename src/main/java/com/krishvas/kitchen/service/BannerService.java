package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.HeroBannerRequest;
import com.krishvas.kitchen.entity.HeroBanner;
import com.krishvas.kitchen.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final HeroBannerRepository heroBannerRepository;

    public List<HeroBanner> activeBanners() {
        return heroBannerRepository.findByActiveTrueOrderByPositionOrderAsc();
    }

    public HeroBanner save(HeroBannerRequest request) {
        HeroBanner banner = new HeroBanner();
        banner.setTitle(request.title());
        banner.setImageUrl(request.imageUrl());
        banner.setActionLabel(request.actionLabel());
        banner.setPositionOrder(request.positionOrder());
        banner.setActive(request.active());
        return heroBannerRepository.save(banner);
    }
}
