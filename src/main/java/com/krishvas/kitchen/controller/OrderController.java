package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.PlaceOrderRequest;
import com.krishvas.kitchen.dto.UpdateOrderStatusRequest;
import com.krishvas.kitchen.dto.VerifyDeliveryOtpRequest;
import com.krishvas.kitchen.dto.BulkAssignOrdersRequest;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.placeOrder(user, request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> mine(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.userOrders(user));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','DELIVERY_PARTNER')")
    public ResponseEntity<?> byOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getByOrderId(orderId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(orderService.allOrders());
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable String orderId, @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, request.status()));
    }

    @PatchMapping("/{orderId}/assign/{partnerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assign(@PathVariable String orderId, @PathVariable Long partnerId) {
        return ResponseEntity.ok(orderService.assignDelivery(orderId, partnerId));
    }

    @PatchMapping("/assign/bulk/{partnerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignBulk(@PathVariable Long partnerId, @RequestBody BulkAssignOrdersRequest request) {
        return ResponseEntity.ok(orderService.assignDeliveryBulk(partnerId, request.orderIds()));
    }

    @GetMapping("/{orderId}/track")
    public ResponseEntity<?> track(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of("order", orderService.getByOrderId(orderId)));
    }

    @GetMapping("/assigned/me")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<?> assigned(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.assignedOrders(user));
    }

    @PatchMapping("/{orderId}/accept")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<?> acceptAssignedOrder(@PathVariable String orderId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.acceptAssignedDelivery(orderId, user));
    }

    @PatchMapping("/{orderId}/verify-otp")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<?> verifyOtp(
        @PathVariable String orderId,
        @RequestBody VerifyDeliveryOtpRequest request,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.verifyDeliveryOtp(orderId, user, request));
    }
}
