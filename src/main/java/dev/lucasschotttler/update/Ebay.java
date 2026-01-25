package dev.lucasschotttler.update;

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
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.lucasschotttler.database.DatabaseItem;

@Service
public class Ebay {

    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    private static final Ebay ebayService = new Ebay();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    public Map<String, Object> getRefreshToken() {

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
                
                logger.info("✅ Token refreshed successfully");
                logger.info("Full response: {}", response.body());
                return tokenData;
            } else {
                logger.error("❌ Failed to refresh token: {}", response.statusCode());
                logger.error("Response: {}", response.body());
                return null;
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("❌ Request error: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    public static boolean createOrUpdateItem(DatabaseItem dbItem) {
        
        logger.info("Ebay createOrUpdate START: sku: {}", dbItem.sku);

        // Get fresh token
        Map<String, Object> tokenData = ebayService.getRefreshToken();
        
        if (tokenData == null) {
            logger.error("Failed to refresh token for sku: {}", dbItem.sku);
            return false;
        }
        
        final String TOKEN = (String) tokenData.get("access_token");
        //final String TOKEN = System.getenv("EBAY_TOKEN");

        final String API = System.getenv("EBAY_INVENTORY_API_END");
        final String MARKETPLACEID = "EBAY_US";
        final String MERCHANT_LOCATION_KEY = "Default-EBAY_US";

        double package_length = dbItem.package_length != null ? dbItem.package_length : dbItem.length + 1;
        double package_width = dbItem.package_width != null ? dbItem.package_width : dbItem.width + 1;
        double package_height = dbItem.package_height != null ? dbItem.package_height : dbItem.height + 1;
        double package_weight = dbItem.package_weight != null ? dbItem.package_weight : dbItem.weight + 1;

        String description = dbItem.description;
        int quantity = dbItem.custom_quantity != null ? dbItem.custom_quantity : (int) (dbItem.quantity * .66);

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
            imageUrls.add(dbItem.milwaukee_images);
        }
        if (dbItem.lakes_images != null && !dbItem.lakes_images.isEmpty()) {
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
        
        String url = API + "inventory_item/" + dbItem.sku;

        logger.info("Ebay createOrUpdate 1: sku: {}", dbItem.sku);
        
        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", MARKETPLACEID)
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        if(!ebayService.doRequest(request, dbItem.sku)) {
            logger.error("Ebay Data PUT FAILED. sku: {}", dbItem.sku);
        }

        logger.info("Ebay createOrUpdateItem SUCCESS. SKU: {}", dbItem.sku);
        
        return true;
    }

    private String getOfferId(String sku, String TOKEN){

        final String offer_url = System.getenv("EBAY_INVENTORY_API_END") + "offer?sku=" + sku;

        // Build HTTP request
        HttpRequest inventoryRequest = HttpRequest.newBuilder()
            .uri(URI.create(offer_url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(inventoryRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
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
            } else {
                logger.error("Failed to get offerId for sku: {}, status: {}, body: {}", sku, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Exception while getting offerId for sku: {}, error: {}", sku, e.getMessage());
        }
        return null;
    }

    public static boolean updateItem(DatabaseItem dbItem){

        // Get fresh token
        Map<String, Object> tokenData = ebayService.getRefreshToken();
        
        if (tokenData == null) {
            logger.error("Failed to refresh token for sku: {}", dbItem.sku);
            return false;
        }
        
        final String TOKEN = (String) tokenData.get("access_token");
        final String API = System.getenv("EBAY_INVENTORY_API_END");

        // #region BUILD_INVENTORY_UPDATE
        ObjectNode inventoryUpdateData = mapper.createObjectNode();

        ObjectNode availability = inventoryUpdateData.putObject("availability");
        ObjectNode shipToLocationAvailability = availability.putObject("shipToLocationAvailability");
        ObjectNode fulfillmentTime = shipToLocationAvailability.putObject("fulfillmentTime");

        fulfillmentTime.put("unit", "BUSINESS_DAY");
        fulfillmentTime.put("value", dbItem.fulfillment);

        shipToLocationAvailability.put("quantity", dbItem.custom_quantity != null ? dbItem.custom_quantity : (int) (dbItem.quantity * .66));
        shipToLocationAvailability.put("merchantLocationKey", "Default-EBAY_US");

        // #endregion 

        final String inventory_url = API + "inventory_item/" + dbItem.sku;

        logger.info("InventoryUpdateData: {}", inventoryUpdateData);

        // Build HTTP request
        HttpRequest inventoryRequest = HttpRequest.newBuilder()
            .uri(URI.create(inventory_url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .PUT(HttpRequest.BodyPublishers.ofString(inventoryUpdateData.toString()))
            .build();

        if(!ebayService.doRequest(inventoryRequest, dbItem.sku)){
            logger.error("Returned to updateOffer in error state. SKU: {}", dbItem.sku);
            return false;
        }

        logger.info("Success updateOffer 1/2. SKU: {}", dbItem.sku);

        ObjectNode offerUpdateNode = mapper.createObjectNode();

        ObjectNode pricingSummary = offerUpdateNode.putObject("pricingSummary");
        ObjectNode price = pricingSummary.putObject("price");

        offerUpdateNode.put("availableQuantity", dbItem.custom_quantity != null ? dbItem.custom_quantity : (int) (dbItem.quantity * .66));

        price.put("currency", "USD");
        price.put("value", dbItem.custom_price != null ? dbItem.custom_price : dbItem.calculated_price);

        final String offer_url = API + "offer/" +  ebayService.getOfferId(dbItem.sku, TOKEN);

        // Build HTTP request
        HttpRequest offerRequest = HttpRequest.newBuilder()
            .uri(URI.create(offer_url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Content-Type", "application/json")
            .header("Content-Language", "en-US")
            .header("X-EBAY-C-MARKETPLACE-ID", "EBAY-US")
            .PUT(HttpRequest.BodyPublishers.ofString(inventoryUpdateData.toString()))
            .build();

        if(!ebayService.doRequest(offerRequest, dbItem.sku)){
            logger.error("Returned to updateOffer in error state. SKU: {}", dbItem.sku);
            return false;
        }

        logger.info("Success updateOffer 2/2. SKU: {}", dbItem.sku);

        return true;
    }

    private boolean doRequest(HttpRequest request, String sku){

        int max_retries = 3;

        for (int i = 0; i < max_retries; i++) {

            logger.info("Ebay Data PUT ATTEMPT: sku: {}, attempt: {}", sku, i + 1);

            HttpResponse<String> response;

            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("Ebay Data PUT FINISHED: sku: {}", sku);
            } catch (Exception e) {
                logger.error("Ebay Data PUT EXCEPTION: sku: {}, attempt: {}, error: {}", sku, i + 1, e.getMessage());
                continue;
            }

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                logger.info("Ebay Data PUT SUCCESS: sku: {}", sku);
                return true;
            } else {
                logger.error("Ebay Data PUT ERROR: sku: {}, status: {}, attempt: {}, body: {}", 
                    sku, response.statusCode(), i + 1, response.body());
                
                // Don't retry on client errors (4xx)
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    logger.error("Client error - not retrying: sku: {}, status: {}", sku, response.statusCode());
                    return false;
                }
                
                double wait = Math.pow(2, i) + Math.random();
                try {
                    Thread.sleep((long)(wait * 1000));
                } catch (InterruptedException ie) {
                    logger.error("Sleep interrupted: sku: {}, attempt: {}", sku, i + 1);
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
