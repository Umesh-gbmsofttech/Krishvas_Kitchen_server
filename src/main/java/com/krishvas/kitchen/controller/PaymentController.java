package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.PaymentRequest;
import com.krishvas.kitchen.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> pay(@PathVariable String orderId, @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.record(orderId, request));
    }
}
