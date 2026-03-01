package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.TrackingUpdateRequest;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<?> push(
        @PathVariable String orderId,
        @RequestBody TrackingUpdateRequest request,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(trackingService.push(orderId, user, request));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> history(@PathVariable String orderId) {
        return ResponseEntity.ok(trackingService.history(orderId));
    }
}
