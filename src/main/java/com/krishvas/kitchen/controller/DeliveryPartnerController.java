package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.DeliveryPartnerApplyRequest;
import com.krishvas.kitchen.dto.DeliveryPartnerDecisionRequest;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery-partners")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DeliveryPartnerController {

    private final DeliveryService deliveryService;

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> apply(@RequestBody DeliveryPartnerApplyRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(deliveryService.apply(user, request));
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> myStatus(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(deliveryService.myStatus(user));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pending() {
        return ResponseEntity.ok(deliveryService.pendingRequests());
    }

    @PatchMapping("/{partnerId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> decision(@PathVariable Long partnerId, @RequestBody DeliveryPartnerDecisionRequest request) {
        return ResponseEntity.ok(deliveryService.decide(partnerId, request));
    }

    @PatchMapping("/{partnerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePartner(@PathVariable Long partnerId, @RequestBody DeliveryPartnerDecisionRequest request) {
        return ResponseEntity.ok(deliveryService.updatePartner(partnerId, request));
    }

    @GetMapping("/approved")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approved() {
        return ResponseEntity.ok(deliveryService.approvedPartners());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(deliveryService.dashboard(user));
    }
}
