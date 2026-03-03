package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.HeroBannerRequest;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.OrderRepository;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.service.AdminService;
import com.krishvas.kitchen.service.BannerService;
import com.krishvas.kitchen.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryService deliveryService;
    private final BannerService bannerService;
    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> dashboard(@AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(adminService.dashboard(adminUser));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> settings(@AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(adminService.settings(adminUser));
    }

    @PatchMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSettings(
        @AuthenticationPrincipal User adminUser,
        @RequestBody Map<String, Object> payload
    ) {
        Boolean kitchenActive = payload.containsKey("kitchenActive") ? Boolean.valueOf(String.valueOf(payload.get("kitchenActive"))) : null;
        Boolean darkMode = payload.containsKey("darkMode") ? Boolean.valueOf(String.valueOf(payload.get("darkMode"))) : null;
        return ResponseEntity.ok(adminService.updateSettings(adminUser, kitchenActive, darkMode));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBanner(@RequestBody HeroBannerRequest request) {
        return ResponseEntity.ok(bannerService.save(request));
    }

    @GetMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> allBanners() {
        return ResponseEntity.ok(bannerService.allBanners());
    }

    @DeleteMapping("/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable Long bannerId) {
        bannerService.delete(bannerId);
        return ResponseEntity.ok().build();
    }
}
