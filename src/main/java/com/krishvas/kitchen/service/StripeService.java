package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.StripeCreateIntentRequest;
import com.krishvas.kitchen.dto.StripeCreateIntentResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${stripe.currency:gbp}")
    private String stripeCurrency;

    private void configureStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalArgumentException("Stripe is not configured");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    public StripeCreateIntentResponse createPaymentIntent(StripeCreateIntentRequest request, Long userId) {
        if (request == null || request.amountMinor() == null || request.amountMinor() <= 0) {
            throw new IllegalArgumentException("Valid amount is required");
        }
        configureStripe();
        try {
            String currency = (request.currency() == null || request.currency().isBlank())
                ? stripeCurrency
                : request.currency().toLowerCase(Locale.ROOT);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", String.valueOf(userId));
            if (request.orderRef() != null && !request.orderRef().isBlank()) {
                metadata.put("orderRef", request.orderRef());
            }
            if (request.paymentMethodPreference() != null && !request.paymentMethodPreference().isBlank()) {
                metadata.put("paymentMethodPreference", request.paymentMethodPreference());
            }
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.amountMinor())
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .putAllMetadata(metadata)
                .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return new StripeCreateIntentResponse(intent.getId(), intent.getClientSecret(), intent.getCurrency(), intent.getAmount());
        } catch (StripeException e) {
            throw new IllegalArgumentException("Unable to create Stripe payment intent");
        }
    }

    public PaymentIntent validateSucceededIntent(String paymentIntentId, Long expectedAmountMinor) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe payment intent id is required");
        }
        configureStripe();
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
                throw new IllegalArgumentException("Payment is not completed");
            }
            if (expectedAmountMinor != null && !expectedAmountMinor.equals(intent.getAmountReceived())) {
                throw new IllegalArgumentException("Paid amount mismatch");
            }
            return intent;
        } catch (StripeException e) {
            throw new IllegalArgumentException("Unable to verify payment");
        }
    }

    public void handleWebhook(String payload, String signature) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            log.warn("Stripe webhook secret not configured; event skipped");
            return;
        }
        try {
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
            String type = event.getType();
            if ("payment_intent.succeeded".equals(type)) {
                log.info("Stripe webhook: payment_intent.succeeded");
            } else if ("payment_intent.payment_failed".equals(type)) {
                log.warn("Stripe webhook: payment_intent.payment_failed");
            } else if ("charge.refunded".equals(type)) {
                log.info("Stripe webhook: charge.refunded");
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }
    }
}
