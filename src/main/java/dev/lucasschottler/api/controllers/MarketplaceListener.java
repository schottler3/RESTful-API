package dev.lucasschottler.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.api.update.Actions;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.marketplaces.ingresTypes.EbayOrderConfirmation;
import dev.lucasschottler.marketplaces.util.JsonToData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/superior/marketplace")
class MarketplaceListener {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceListener.class);
    private final Actions actions;
    private final Databasing db;

    private String verificationToken = System.getenv("EBAY_VERIFICATION_TOKEN");

    private String endpointUrl = System.getenv("EBAY_ENDPOINT_URL");

    public MarketplaceListener(Actions actions, Databasing db){
        this.actions = actions;
        this.db = db;
    }

    @GetMapping("/{sku}") 
    public ResponseEntity<String> manualSquareUpdate(@PathVariable String sku, @RequestParam(required = true) int quantity){
        if(actions.updateSquareInventory(sku, quantity)){
            return ResponseEntity.ok("Success");
        } else {
            return ResponseEntity.status(400).body("Failure");
        }
    }

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
    public ResponseEntity<Void> handleEbayEvent(@RequestBody String rawJson) {
        EbayOrderConfirmation order = JsonToData.parseEbayOrderConfirmation(rawJson);

        logger.info("Marketplaces: Ebay order: {}", rawJson);
    
        if (order == null) {
            return ResponseEntity.badRequest().build();
        }
    
        //actions.updateSquareInventory(db.addEbayOrder(order));
    
        return ResponseEntity.noContent().build();
    }
}