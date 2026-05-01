package dev.lucasschottler.update;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.fulfillment.Policies;

import com.fasterxml.jackson.databind.node.ArrayNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Ebay {

    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    private static final Ebay ebayService = new Ebay();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Policies policies = new Policies();

    private String globalAccessToken;
    
    private synchronized void refreshToken() {

        // Get required credentials from environment
        String refreshToken = System.getenv("EBAY_REFRESH_TOKEN");
        String clientId = System.getenv("EBAY_APPID");
        String clientSecret = System.getenv("EBAY_CERTID");
        String identityEnd = System.getenv("EBAY_IDENTITY_END");
        
        // Encode credentials using base64
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        
        // DON'T REQUEST SCOPES - let it use the refresh token's scopes
        String requestBody = String.format(
            "grant_type=%s&refresh_token=%s",
            URLEncoder.encode("refresh_token", StandardCharsets.UTF_8),
            URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
        );
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(identityEnd))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + encodedCredentials)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        // Create HttpClient instance
        HttpClient httpClient = HttpClient.newHttpClient();
        
        try {
            // Send the request
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> tokenData = objectMapper.readValue(
                    response.body(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                
                logger.info("✅ Ebay: Token refreshed successfully");
                
                if(tokenData != null){
                    ebayService.globalAccessToken = (String) tokenData.get("access_token");
                }

            } else {
                logger.error("❌ Failed to refresh token: {}", response.statusCode());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("❌ Request error: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static boolean createOrUpdateItem(DatabaseItem dbItem) {

        if (ebayService.globalAccessToken == null) {
            ebayService.refreshToken();
        }
        
        //logger.info("Ebay createOrUpdate START: sku: {}", dbItem.sku);

        final String API = System.getenv("EBAY_INVENTORY_API_END");
        final String MARKETPLACEID = "EBAY_US";
        final String MERCHANT_LOCATION_KEY = "Default-EBAY_US";

        double package_length = dbItem.package_length != null ? dbItem.package_length : dbItem.length + 1;
        double package_width = dbItem.package_width != null ? dbItem.package_width : dbItem.width + 1;
        double package_height = dbItem.package_height != null ? dbItem.package_height : dbItem.height + 1;
        double package_weight = dbItem.package_weight != null ? dbItem.package_weight : dbItem.weight + 1;

        String description = dbItem.description;

        if(description == null || description.isEmpty()){
            logger.error("Ebay: Skipping sku: {} — description is null or empty", dbItem.sku);
            Webhook.sendMessage(String.format("Ebay: Missing description for sku: %s, \nhttps://app.lucasschottler.dev/admin/inventory/%s", dbItem.sku, dbItem.sku));
            return false;
        }

        //logger.info("Ebay: Description length for sku: {}: {}", dbItem.sku, description.length());

        Integer custom_quantity = dbItem.custom_quantity;
        int lakes_quantity = dbItem.quantity;

        //logger.info("Ebay: Custom_quantity: {} lakes_quantity: {}", custom_quantity, lakes_quantity);

        int quantity = custom_quantity != null && custom_quantity > 0 
            ? custom_quantity 
            : (int) (lakes_quantity * 0.66);

        // #region JSON_BUILD

        // Build product object
        ObjectNode product = mapper.createObjectNode();
        
        // Build aspects
        ObjectNode aspects = product.putObject("aspects");
        aspects.putArray("width").add(String.valueOf(dbItem.width));
        aspects.putArray("length").add(String.valueOf(dbItem.length));
        aspects.putArray("height").add(String.valueOf(dbItem.height));
        aspects.putArray("weight").add(String.valueOf(dbItem.weight));
        aspects.putArray("Brand").add("Milwaukee");
        aspects.putArray("Part Type").add(dbItem.type != null ? dbItem.type : "");
        aspects.putArray("Type").add(dbItem.type != null ? dbItem.type : "");
        
        // Add other product fields
        product.put("brand", "Milwaukee");
        product.put("mpn", dbItem.mpn != null ? dbItem.mpn : "");
        product.put("title", dbItem.title != null ? dbItem.title : "");
        product.put("description", description != null ? description : "");
        
        // Add UPC array
        ArrayNode upcArray = product.putArray("upc");
        if (dbItem.upc != null && !dbItem.upc.isEmpty()) {
            upcArray.add(dbItem.upc);
        }
        
        // Add image URLs
        ArrayNode imageUrls = product.putArray("imageUrls");
        if (dbItem.milwaukee_images != null && !dbItem.milwaukee_images.isEmpty()) {
            for (String url : dbItem.milwaukee_images.split(",")) {
                imageUrls.add(url.trim());
            }
        } else {
            imageUrls.add(dbItem.lakes_images);
        }
        
        // Build root object
        ObjectNode root = mapper.createObjectNode();
        root.set("product", product);
        root.put("condition", "NEW");
        
        // Build packageWeightAndSize
        ObjectNode packageWeightAndSize = root.putObject("packageWeightAndSize");
        
        ObjectNode dimensions = packageWeightAndSize.putObject("dimensions");
        dimensions.put("height", package_height);
        dimensions.put("length", package_length);
        dimensions.put("width", package_width);
        dimensions.put("unit", "INCH");
        
        ObjectNode weight = packageWeightAndSize.putObject("weight");
        weight.put("value", package_weight);
        weight.put("unit", "POUND");
        
        // Build availability
        ObjectNode availability = root.putObject("availability");
        ObjectNode shipToLocationAvailability = availability.putObject("shipToLocationAvailability");
        
        ObjectNode fulfillmentTime = shipToLocationAvailability.putObject("fulfillmentTime");
        fulfillmentTime.put("unit", "BUSINESS_DAY");
        fulfillmentTime.put("value", dbItem.fulfillment != null ? dbItem.fulfillment : 3);
        
        shipToLocationAvailability.put("quantity", quantity);
        shipToLocationAvailability.put("merchantLocationKey", MERCHANT_LOCATION_KEY);
        
        // #endregion
        
        // Convert to JSON string
        String jsonBody;
        try {
            jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            logger.error("Failed to serialize JSON for sku: {}, error: {}", dbItem.sku, e.getMessage());
            return false;
        }
        
        String url = API + "inventory_item/" + dbItem.sku.trim().replace(" ", "%20");

        //logger.info("Ebay createOrUpdate 1: sku: {}", dbItem.sku);

        //logger.info("Ebay createOrUpdate body: {}", jsonBody);

        return ebayService.doRequest(() -> HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + ebayService.globalAccessToken)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", MARKETPLACEID)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build(), dbItem.sku);
    }

    private String getOfferId(String sku) {
        final String offer_url = System.getenv("EBAY_INVENTORY_API_END") + "offer?sku=" + sku.trim().replace(" ", "%20");

        HttpResponse<String> response = doRequestForResponse(() -> HttpRequest.newBuilder()
            .uri(URI.create(offer_url))
            .header("Authorization", "Bearer " + ebayService.globalAccessToken)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .GET()
            .build(), sku);

        if (response == null) return null;

        try {
            Map<String, Object> responseMap = mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if (responseMap.containsKey("offers")) {
                Object offersObj = responseMap.get("offers");
                if (offersObj instanceof java.util.List && !((java.util.List<?>) offersObj).isEmpty()) {
                    Object firstOffer = ((java.util.List<?>) offersObj).get(0);
                    if (firstOffer instanceof Map) {
                        Object offerId = ((Map<?, ?>) firstOffer).get("offerId");
                        return offerId != null ? offerId.toString() : null;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception parsing offerId for sku: {}, error: {}", sku, e.getMessage());
        }
        return null;
    }

    public static boolean updateItem(DatabaseItem dbItem){
        
        if (ebayService.globalAccessToken == null) {
            ebayService.refreshToken();
        }
        
        final String API = System.getenv("EBAY_INVENTORY_API_END");

        // #region BUILD_INVENTORY_UPDATE
        ObjectNode inventoryUpdateData = mapper.createObjectNode();

        ObjectNode availability = inventoryUpdateData.putObject("availability");
        ObjectNode shipToLocationAvailability = availability.putObject("shipToLocationAvailability");
        ObjectNode fulfillmentTime = shipToLocationAvailability.putObject("fulfillmentTime");

        fulfillmentTime.put("unit", "BUSINESS_DAY");
        fulfillmentTime.put("value", dbItem.fulfillment);

        shipToLocationAvailability.put("quantity", dbItem.custom_quantity != null && dbItem.custom_quantity > 0 ? dbItem.custom_quantity : (int) (dbItem.quantity * .66));
        shipToLocationAvailability.put("merchantLocationKey", "Default-EBAY_US");

        // #endregion 

        final String inventory_url = API + "inventory_item/" + dbItem.sku.trim().replace(" ", "%20");

        //logger.info("InventoryUpdateData: {}", inventoryUpdateData);

        return ebayService.doRequest(() -> HttpRequest.newBuilder()
            .uri(URI.create(inventory_url))
            .header("Authorization", "Bearer " + ebayService.globalAccessToken)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .PUT(HttpRequest.BodyPublishers.ofString(inventoryUpdateData.toString()))
            .build(), dbItem.sku);
    }

    public static boolean updateOffer(DatabaseItem dbItem) {

        if (ebayService.globalAccessToken == null) {
            ebayService.refreshToken();
        }

        final String API = System.getenv("EBAY_INVENTORY_API_END");

        ObjectNode offerUpdateNode = mapper.createObjectNode();

        // Pricing
        ObjectNode pricingSummary = offerUpdateNode.putObject("pricingSummary");
        ObjectNode price = pricingSummary.putObject("price");
        price.put("currency", "USD");
        price.put("value", dbItem.calculated_price);

        // Quantity
        offerUpdateNode.put("availableQuantity",
            dbItem.custom_quantity != null && dbItem.custom_quantity > 0
                ? dbItem.custom_quantity
                : (int) (dbItem.quantity * .66));

        // Fulfillment policy
        Policies.FulfillmentPolicy fulfillmentPolicy = policies.getFulfillmentPolicy(
            dbItem.fulfillment,
            dbItem.package_weight != null ? dbItem.package_weight : dbItem.weight,
            dbItem.package_length != null ? dbItem.package_length : dbItem.length,
            dbItem.package_width != null ? dbItem.package_width : dbItem.width,
            dbItem.package_height != null ? dbItem.package_height : dbItem.height
        );

        ObjectNode listingPolicies = offerUpdateNode.putObject("listingPolicies");
        listingPolicies.put("fulfillmentPolicyId", fulfillmentPolicy.getId());

        // Offer ID
        String offerId = ebayService.getOfferId(dbItem.sku.trim().replace(" ", "%20"));

        if (offerId == null) {
            logger.error("Ebay: Could not retrieve offerId for sku: {}, aborting updateOffer", dbItem.sku);
            return false;
        }

        logger.info("OfferUpdate: {}", offerUpdateNode.toString());

        final String offer_url = API + "offer/" + offerId;

        return ebayService.doRequest(() -> HttpRequest.newBuilder()
            .uri(URI.create(offer_url))
            .header("Authorization", "Bearer " + ebayService.globalAccessToken)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .PUT(HttpRequest.BodyPublishers.ofString(offerUpdateNode.toString()))
            .build(), dbItem.sku);
    }

    private boolean doRequest(java.util.function.Supplier<HttpRequest> requestSupplier, String sku) {

        int max_retries = 3;

        for (int i = 0; i < max_retries; i++) {
            HttpRequest request = requestSupplier.get();

            //logger.info("Ebay Data PUT ATTEMPT: sku: {}, attempt: {}", sku, i + 1);
            HttpResponse<String> response = null;

            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                //logger.info("Ebay Data PUT FINISHED: sku: {}", sku);
            } catch (Exception e) {
                logger.error("Ebay Data PUT EXCEPTION: sku: {}, attempt: {}, error: {}", 
                    sku, i + 1, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                
                // Backoff before retrying on network errors
                try {
                    long waitMs = (long)(Math.pow(2, i) * 1000);
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            if(response != null){

                int status = response.statusCode();

                switch(status){
                    case 200:
                    case 204:
                        return true;
                    case 400:
                        String body = response.body();
                        if (body.contains("25007")) {
                            logger.warn("Ebay: Fulfillment policy violation for sku: {}, body: {}", sku, body);
                            Webhook.sendFulfillmentMessage(String.format("Ebay: Fulfillment policy violation (check shipping policy/weight): sku: %s, body: %s, \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, body, sku));
                        } else {
                            logger.warn("Ebay: Item has missing data! sku: {}, body: {}", sku, body);
                            Webhook.sendMessage(String.format("Ebay: Missing data sku: %s, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, body, sku));
                        }
                        return false;
                    case 401:
                        refreshToken();
                        continue;
                    case 404:
                        logger.warn("Ebay: 404 not found, sku: {}", sku);
                        return false;
                    case 422:
                        logger.error("Ebay: Invalid request data, sku: {}, body: {}", sku, response.body());
                        Webhook.sendMessage(String.format("Ebay: 422 sku: %s, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, response.body(), sku));
                        return false;
                    case 429:
                        try {
                            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                            long waitMs = retryAfter != null
                                ? Long.parseLong(retryAfter) * 1000
                                : (long)(Math.pow(2, i) * 1000);
                            logger.warn("Ebay: Rate limited, waiting {}ms. sku: {}", waitMs, sku);
                            Thread.sleep(waitMs);
                        } catch (Exception ex) {
                            logger.error("Ebay: Sleep failed: {}", ex.getMessage());
                        }
                        continue;
                    default:
                        if (status >= 500) {
                            logger.error("Ebay: Server error, will retry. sku: {}, status: {}, body: {}", sku, status, response.body());
                            double wait = Math.pow(2, i) + Math.random();
                            try {
                                Thread.sleep((long)(wait * 1000));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return false;
                            }
                            continue;
                        }
                        logger.error("Ebay: Unhandled client error, not retrying. sku: {}, status: {}, body: {}", sku, status, response.body());
                        Webhook.sendMessage(String.format("Ebay: Unhandled client error, not retrying. sku: %s, status: %d, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, status, response.body(), sku));
                        return false;
                }
            }
        }

        logger.info("Ebay: Failed poison!: sku: {}, attempt: {}", sku, max_retries);
        Webhook.sendMessage(String.format("Ebay: Failed poison!: sku: %s, attempt: %d \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, max_retries, sku));

        return false;
    }

    private HttpResponse<String> doRequestForResponse(java.util.function.Supplier<HttpRequest> requestSupplier, String sku) {
        int max_retries = 3;

        for (int i = 0; i < max_retries; i++) {
            HttpRequest request = requestSupplier.get();
            HttpResponse<String> response = null;

            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                logger.error("Ebay Data PUT EXCEPTION: sku: {}, attempt: {}, error: {}", 
                    sku, i + 1, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                
                // Backoff before retrying on network errors
                try {
                    long waitMs = (long)(Math.pow(2, i) * 1000);
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            if (response != null) {
                int status = response.statusCode();

                switch (status) {
                    case 200:
                    case 204:
                        return response;
                    case 400:
                        String body = response.body();
                        if (body.contains("25020")) {
                            logger.warn("Ebay: Fulfillment policy violation for sku: {}, body: {}", sku, body);
                            Webhook.sendFulfillmentMessage(String.format("Ebay: Fulfillment policy violation (check shipping policy/weight): sku: %s, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, body, sku));
                        } else {
                            logger.warn("Ebay: Item has missing data! sku: {}, body: {}", sku, body);
                            Webhook.sendMessage(String.format("Ebay: Missing data sku: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, body, sku));
                        }
                        return null;
                    case 401:
                        refreshToken();
                        continue;
                    case 404:
                        logger.warn("Ebay: 404 not found, sku: {}", sku);
                        return null;
                    case 422:
                        logger.error("Ebay: Invalid request data, sku: {}, body: {}", sku, response.body());
                        Webhook.sendMessage(String.format("Ebay: 422 sku: %s, body: %s", sku, response.body()));
                        return null;
                    case 429:
                        try {
                            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                            long waitMs = retryAfter != null
                                ? Long.parseLong(retryAfter) * 1000
                                : (long)(Math.pow(2, i) * 1000);
                            logger.warn("Ebay: Rate limited, waiting {}ms. sku: {}", waitMs, sku);
                            Thread.sleep(waitMs);
                        } catch (Exception ex) {
                            logger.error("Ebay: Sleep failed: {}", ex.getMessage());
                        }
                        continue;
                    default:
                        if (status >= 500) {
                            logger.error("Ebay: Server error, will retry. sku: {}, status: {}, body: {}", sku, status, response.body());
                            double wait = Math.pow(2, i) + Math.random();
                            try {
                                Thread.sleep((long)(wait * 1000));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return null;
                            }
                            continue;
                        }
                        logger.error("Ebay: Unhandled client error, not retrying. sku: {}, status: {}, body: {}", sku, status, response.body());
                        Webhook.sendMessage(String.format("Ebay: Unhandled client error, not retrying. sku: %s, status: %d, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, status, response.body(), sku));
                        return null;
                }
            }
        }
        Webhook.sendMessage(String.format("Ebay: Failed poison!: sku: %s, attempt: %d \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, max_retries, sku));
        return null;
    }
}
