package dev.lucasschottler.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
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
            // eBay requires: HMAC-SHA256(challengeCode + verificationToken + endpointUrl)
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(verificationToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(challengeCode.getBytes(StandardCharsets.UTF_8));
            mac.update(verificationToken.getBytes(StandardCharsets.UTF_8));
            byte[] hash = mac.doFinal(endpointUrl.getBytes(StandardCharsets.UTF_8));

            String challengeResponse = HexFormat.of().formatHex(hash);
            logger.info("eBay endpoint verification successful");
            return ResponseEntity.ok(Map.of("challengeResponse", challengeResponse));

        } catch (Exception e) {
            logger.error("eBay endpoint verification failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Step 2: eBay sends real events via POST
    @PostMapping("/ebay")
    public ResponseEntity<Void> handleEbayEvent(@RequestBody Map<String, Object> body) {
        logger.info("eBay event received: {}", body);

        // TODO: verify eBay's X-EBAY-SIGNATURE header here for production
        // https://developer.ebay.com/api-docs/commerce/notification/overview.html

        return ResponseEntity.noContent().build();
    }
}