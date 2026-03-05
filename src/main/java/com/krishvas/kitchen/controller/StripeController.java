package com.krishvas.kitchen.controller;

import com.krishvas.kitchen.dto.StripeCreateIntentRequest;
import com.krishvas.kitchen.service.StripeService;
import com.krishvas.kitchen.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;

    @PostMapping("/create-intent")
    @PreAuthorize("hasAnyRole('USER','DELIVERY_PARTNER')")
    public ResponseEntity<?> createIntent(@RequestBody StripeCreateIntentRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(stripeService.createPaymentIntent(request, user.getId()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        stripeService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("");
    }
}
