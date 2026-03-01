package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.HeroBannerRequest;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.OrderRepository;
import com.krishvas.kitchen.repository.UserRepository;
import com.krishvas.kitchen.service.BannerService;
import com.krishvas.kitchen.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(Map.of(
            "totalOrders", orderRepository.count(),
            "totalUsers", userRepository.count(),
            "pendingDeliveryPartners", deliveryService.pendingRequests().size()
        ));
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
}
