package com.kaycheung.payment_service.controller;

import com.kaycheung.payment_service.config.properties.StripeProperties;
import com.kaycheung.payment_service.exception.BadWebhookPayloadException;
import com.kaycheung.payment_service.service.WebhooksService;
import com.nimbusds.jose.shaded.gson.JsonSyntaxException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhooksController {
    private static final Logger log = LoggerFactory.getLogger(WebhooksController.class);
    private final WebhooksService webhooksService;
    private final StripeProperties stripeProperties;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripWebhooks(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

//        log.warn("Stripe payload={}", payload);

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSigningSecret());
            log.warn("Stripe webhook eventId={} type={} eventApiVersion={} sdkApiVersion={}",
                    event.getId(), event.getType(), event.getApiVersion(), com.stripe.Stripe.API_VERSION);
        } catch (SignatureVerificationException ex) {
            //  Invalid signature
            log.warn("SignatureVerificationException={} ", ex.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (JsonSyntaxException ex) {
            //  Invalid payload
            log.warn("JsonSyntaxException={} ", ex.getMessage());
            return ResponseEntity.badRequest().body("Bad request");
        }

        try {
            webhooksService.handleStripeWebhookEvent(event, payload);
        } catch (BadWebhookPayloadException ex) {
            log.error("BadWebhookPayloadException:", ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            log.error("Webhook processing failed", ex);
            return ResponseEntity.status(500).body("Server error");
        }

        return ResponseEntity.ok("ok");
    }
}
