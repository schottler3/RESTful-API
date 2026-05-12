package dev.lucasschottler.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.lucasschottler.api.Webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/superior/marketplace")
class MarketplaceListener {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceListener.class);

    private String verificationToken = System.getenv("EBAY_VERIFICATION_TOKEN");

    private String endpointUrl = System.getenv("EBAY_ENDPOINT_URL");

    // Step 1: eBay calls GET first to verify you own the endpoint
    @GetMapping("/ebay")
    public ResponseEntity<Map<String, String>> verifyEbayEndpoint(
            @RequestParam("challenge_code") String challengeCode) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(challengeCode.getBytes(StandardCharsets.UTF_8));
            digest.update(verificationToken.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = digest.digest(endpointUrl.getBytes(StandardCharsets.UTF_8));

            String challengeResponse = HexFormat.of().formatHex(bytes);
            logger.info("challengeResponse: '{}'", challengeResponse);
            return ResponseEntity.ok(Map.of("challengeResponse", challengeResponse));

        } catch (Exception e) {
            logger.error("eBay endpoint verification failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Step 2: eBay sends real events via POST
    @PostMapping("/ebay")
    public ResponseEntity<Void> handleEbayEvent(@RequestBody Map<String, Object> body) {
        Map<String, Object> notification = (Map<String, Object>) body.get("notification");
        Map<String, Object> data = (Map<String, Object>) notification.get("data");
        Map<String, Object> order = (Map<String, Object>) data.get("order");

        String orderId = (String) order.get("orderId");
        List<Map<String, Object>> lineItems = (List<Map<String, Object>>) order.get("orderLineItems");

        for (Map<String, Object> lineItem : lineItems) {
            String listingId = (String) lineItem.get("listingId");
            Integer quantity = (Integer) lineItem.get("quantity");

            Webhook.sendEbayMessage(String.format("Order: {}, ListingId: {}, Quantity: {}", orderId, listingId, quantity));
        }

        return ResponseEntity.noContent().build();
    }
}