package dev.lucasschottler.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.database.DatabaseItem;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Service
public class Amazon {

    private final static String SELLER_ID = System.getenv("AMAZON_SELLER_ID");
    private static final Logger logger = LoggerFactory.getLogger(Amazon.class);
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private final static String ENDPOINT = "https://sellingpartnerapi-na.amazon.com";
    private final static String MARKETPLACE_ID = "ATVPDKIKX0DER";

    public class AmazonNotFoundException extends Exception{
        public AmazonNotFoundException(String sku) {
            super("Amazon item not found! sku: " + sku);  
        }
    }

    private String accessToken;

    public static HashMap<String, Double> getPrices(double basePrice) {

        HashMap<String, Double> amazonPrices = new HashMap<>();

        double minimum_price = 999.99;
        double middle_price = 999.99;
        double maximum_price = 999.99;

        if (basePrice < 10) {
            minimum_price = basePrice * 2.062;
            middle_price = minimum_price + 6.5;
            maximum_price = minimum_price * 4.2;
        } else if (basePrice < 25) {
            minimum_price = basePrice * 1.622;
            middle_price = minimum_price + 7.5;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 100) {
            minimum_price = basePrice * 1.342;
            middle_price = minimum_price + 8;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 200) {
            minimum_price = basePrice * 1.347;
            middle_price = minimum_price + 12;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 500) {
            minimum_price = basePrice * 1.272;
            middle_price = minimum_price + 15;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 1000) {
            minimum_price = basePrice * 1.282;
            middle_price = minimum_price + 20;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 5000) {
            minimum_price = basePrice * 1.27;
            middle_price = minimum_price + 40;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 10000) {
            minimum_price = basePrice * 1.222;
            middle_price = minimum_price + 100;
            maximum_price = minimum_price * 3;
        } else {
            minimum_price = basePrice * 1.3;
            middle_price = minimum_price + 200;
            maximum_price = minimum_price * 3;
        }

        // Round prices to 2 decimal places
        minimum_price = Math.round(minimum_price * 100.0) / 100.0;
        middle_price = Math.round(middle_price * 100.0) / 100.0;
        maximum_price = Math.round(maximum_price * 100.0) / 100.0;

        // Ensure prices are not less than 2.5
        if (minimum_price < 2.5) minimum_price = 2.5;
        if (middle_price < 2.5) middle_price = 2.5;
        if (maximum_price < 2.5) maximum_price = 2.5;

        // Add prices to the HashMap
        amazonPrices.put("minimum_price", minimum_price);
        amazonPrices.put("middle_price", middle_price);
        amazonPrices.put("maximum_price", maximum_price);

        return amazonPrices;
    }

    public boolean updateItem(DatabaseItem dbItem) throws AmazonNotFoundException{
        
        if(accessToken == null || accessToken.equals("")) {
            refreshToken();
        }

        final String get_url = ENDPOINT + "/listings/2021-08-01/items/" + SELLER_ID + "/" 
            + dbItem.sku.trim().replace(" ", "%20") 
            + "?marketplaceIds=" + MARKETPLACE_ID + "&issueLocale=en_US&includedData=attributes";

        //logger.info("Amazon getUrl = {}", get_url);

        HttpResponse<String> get_response = doRequest(() -> HttpRequest.newBuilder()
            .uri(URI.create(get_url))
            .header("accept", "application/json")
            .header("x-amz-access-token", this.accessToken)
            .GET()
            .build(), dbItem.sku);

        if (get_response == null) {
            logger.error("Amazon updateItem got null response, sku: {}", dbItem.sku);

            return false;
        }

        //logger.info("Amazon updateItem getItem returned: {}", get_response.statusCode());

        if(get_response.statusCode() == 404){
            //logger.warn("Amazon item not found or doesn't exist. sku: {}", dbItem.sku);

            /**TODO:
             * Amazon opportunities report
             * Implement databasing logic for all items that are not on amazon right now but
             * could be added to the marketplace manually.
             * https://superiortool.atlassian.net/jira/software/projects/DEV/boards/2?selectedIssue=DEV-26
            **/

            throw new AmazonNotFoundException(dbItem.sku);
        }

        ObjectNode itemData;

        try{
            itemData = (ObjectNode) mapper.readTree(get_response.body());
        } catch (Exception e){
            logger.error("Amazon JSON mapper error on get_response, sku: {}", dbItem.sku);

            Webhook.sendMessage(String.format("Amazon JSON mapper error on get_response, sku: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", dbItem.sku, dbItem.sku));

            return false;
        }

        // Get the productType from the response
        String productType = "PRODUCT"; // default fallback
        if (itemData.has("productType")) {
            productType = itemData.get("productType").asText();
            //logger.info("Amazon productType for sku {}: {}", dbItem.sku, productType);
        }
        /* 
        else {
            logger.warn("Amazon productType not found in response for sku: {}, using default", dbItem.sku);
        }
        */

        ObjectNode currentAttributes = null;
        if (itemData.has("attributes") && itemData.get("attributes").isObject()) {
            currentAttributes = (ObjectNode) itemData.get("attributes");
        } else {
            logger.error("Amazon itemData missing 'attributes' field, sku: {}, itemData: {}", dbItem.sku, itemData.toString());

            Webhook.sendMessage(String.format("Amazon itemData missing 'attributes' field, sku: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", dbItem.sku, dbItem.sku));

            return false;
        }

        ArrayNode patches = mapper.createArrayNode();

        // #region Purchaseable offer patch
        if (currentAttributes.has("purchasable_offer")) {
            ObjectNode purchasableOfferPatch = mapper.createObjectNode();
            purchasableOfferPatch.put("op", "replace");
            purchasableOfferPatch.put("path", "/attributes/purchasable_offer");

            ArrayNode purchasableOfferValue = mapper.createArrayNode();
            ObjectNode offer = mapper.createObjectNode();

            // our_price
            ArrayNode ourPrice = mapper.createArrayNode();
            ObjectNode ourPriceSchedule = mapper.createObjectNode();
            ArrayNode ourPriceScheduleArr = mapper.createArrayNode();
            ObjectNode ourPriceValue = mapper.createObjectNode();

            ourPriceValue.put("value_with_tax", dbItem.calculated_price);
            ourPriceScheduleArr.add(ourPriceValue);
            ourPriceSchedule.set("schedule", ourPriceScheduleArr);
            ourPrice.add(ourPriceSchedule);
            offer.set("our_price", ourPrice);

            // minimum_seller_allowed_price
            ArrayNode minPrice = mapper.createArrayNode();
            ObjectNode minPriceSchedule = mapper.createObjectNode();
            ArrayNode minPriceScheduleArr = mapper.createArrayNode();
            ObjectNode minPriceValue = mapper.createObjectNode();
            minPriceValue.put("value_with_tax", dbItem.minimum_price);
            minPriceScheduleArr.add(minPriceValue);
            minPriceSchedule.set("schedule", minPriceScheduleArr);
            minPrice.add(minPriceSchedule);
            offer.set("minimum_seller_allowed_price", minPrice);

            // maximum_seller_allowed_price
            ArrayNode maxPrice = mapper.createArrayNode();
            ObjectNode maxPriceSchedule = mapper.createObjectNode();
            ArrayNode maxPriceScheduleArr = mapper.createArrayNode();
            ObjectNode maxPriceValue = mapper.createObjectNode();
            maxPriceValue.put("value_with_tax", dbItem.maximum_price);
            maxPriceScheduleArr.add(maxPriceValue);
            maxPriceSchedule.set("schedule", maxPriceScheduleArr);
            maxPrice.add(maxPriceSchedule);
            offer.set("maximum_seller_allowed_price", maxPrice);

            purchasableOfferValue.add(offer);
            purchasableOfferPatch.set("value", purchasableOfferValue);

            patches.add(purchasableOfferPatch);
        }
        //#endregion

        // #region Fulfillment availability patch
        ObjectNode fulfillmentPatch = mapper.createObjectNode();
        fulfillmentPatch.put("op", "replace");
        fulfillmentPatch.put("path", "/attributes/fulfillment_availability");

        ArrayNode fulfillmentValue = mapper.createArrayNode();
        ObjectNode fulfillment = mapper.createObjectNode();
        fulfillment.put("fulfillment_channel_code", "DEFAULT");
        fulfillment.put("quantity", dbItem.custom_quantity != null && dbItem.custom_quantity > 0 ? dbItem.custom_quantity : (int) (dbItem.quantity * .66));
        fulfillment.put("lead_time_to_ship_max_days", dbItem.fulfillment);
        fulfillmentValue.add(fulfillment);

        fulfillmentPatch.set("value", fulfillmentValue);

        patches.add(fulfillmentPatch);
        //#endregion

        // #region Images patch

        if(dbItem.milwaukee_images != null){
            String[] milwaukee_images = dbItem.milwaukee_images.split(",");
            
            // Trim whitespace from URLs
            milwaukee_images = java.util.Arrays.stream(milwaukee_images)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toArray(String[]::new);

            if(milwaukee_images.length > 0){
                
                // Set main product image (first image)
                ObjectNode mainImagePatch = mapper.createObjectNode();
                mainImagePatch.put("op", "replace");
                mainImagePatch.put("path", "/attributes/main_product_image_locator");
                
                ArrayNode mainImageArr = mapper.createArrayNode();
                ObjectNode mainImageObj = mapper.createObjectNode();
                mainImageObj.put("media_location", milwaukee_images[0]);
                mainImageObj.put("marketplace_id", MARKETPLACE_ID);
                mainImageArr.add(mainImageObj);
                
                mainImagePatch.set("value", mainImageArr);
                patches.add(mainImagePatch);

                // Set additional images (remaining images)
                int maxAdditionalImages = Math.min(milwaukee_images.length - 1, 8); // Amazon allows up to 8 additional images
                
                for (int i = 0; i < maxAdditionalImages; i++) {
                    ObjectNode imagePatch = mapper.createObjectNode();
                    imagePatch.put("op", "replace");
                    imagePatch.put("path", "/attributes/other_product_image_locator_" + (i + 1));

                    ArrayNode valueArr = mapper.createArrayNode();
                    ObjectNode valueObj = mapper.createObjectNode();
                    valueObj.put("media_location", milwaukee_images[i + 1]); // Start from second image
                    valueObj.put("marketplace_id", MARKETPLACE_ID);
                    valueArr.add(valueObj);

                    imagePatch.set("value", valueArr);

                    patches.add(imagePatch);
                }
            }
        }

        //#endregion

        // Create request body with productType
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("productType", productType);
        requestBody.set("patches", patches);

        //logger.info("Amazon PATCH request body, sku: {}, body: {}", dbItem.sku, requestBody.toString());

        final String patch_url = ENDPOINT + "/listings/2021-08-01/items/" + SELLER_ID + "/" + dbItem.sku.trim().replace(" ", "%20") + "?marketplaceIds=" + MARKETPLACE_ID;

        HttpResponse<String> patch_response = doRequest(() -> HttpRequest.newBuilder()
            .uri(URI.create(patch_url))
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .header("x-amz-access-token", this.accessToken)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build(), dbItem.sku);

        if (patch_response == null) {
            logger.error("Amazon PATCH got null response, sku: {}", dbItem.sku);

            Webhook.sendMessage(String.format("Amazon: Received a null patch response! Sku: %s", dbItem.sku));

            return false;
        }
        if (patch_response.statusCode() != 200 && patch_response.statusCode() != 204) {
            logger.error("Amazon PATCH failed, sku: {}, status: {}", dbItem.sku, patch_response.statusCode());
            Webhook.sendMessage(String.format("Amazon: Failure on patch response! Sku: %s, status: %d", dbItem.sku, patch_response.statusCode()));
            return false;
        }

        //logger.info("Amazon PATCH finished, sku: {}", dbItem.sku);

        return true;
    }

    private synchronized void refreshToken() {
        try {
            String token = System.getenv("AMAZON_TOKEN");
            String clientId = System.getenv("AMAZON_IDENTIFIER");
            String clientSecret = System.getenv("AMAZON_CLIENT_SECRET");

            if (token == null || clientId == null || clientSecret == null) {
                logger.error("Amazon: missing required env vars for token refresh");
                return;
            }

            String formData = "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            HttpResponse<String> response = doRequest(() -> HttpRequest.newBuilder()
                .uri(URI.create("https://api.amazon.com/auth/o2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build(), "ACCESS_TOKEN");

            if (response == null || response.statusCode() != 200) {
                logger.error("Amazon token refresh failed. Status: {}, Body: {}", 
                    response != null ? response.statusCode() : "null",
                    response != null ? response.body() : "null"); // log body for debugging
                return;
            }

            ObjectNode responseJson = (ObjectNode) mapper.readTree(response.body());
            accessToken = responseJson.has("access_token") ? responseJson.get("access_token").asText() : null;

            if (accessToken != null) {
                logger.info("✅ Amazon: Token refreshed successfully");
            } else {
                logger.error("Amazon: Token refresh response missing access_token. Body: {}", response.body());
            }

        } catch (Exception e) {
            logger.error("Amazon refresh_token FAILED. error: {}", e.getMessage());
        }
    }

    private HttpResponse<String> doRequest(java.util.function.Supplier<HttpRequest> requestSupplier, String sku) {
        int max_retries = 3;

        for (int i = 0; i < max_retries; i++) {
            HttpRequest request = requestSupplier.get();
            HttpResponse<String> response = null;

            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                //logger.info("Amazon Request FINISHED: sku: {}", sku);
            } catch (Exception e) {
                logger.error("Amazon Request EXCEPTION: sku: {}, attempt: {}, error: {}", sku, i + 1, e.getMessage());
                continue;
            }

            if (response != null) {
                int status = response.statusCode();

                switch (status) {
                    case 200:
                    case 204:
                        //logger.info("Amazon Request SUCCESS: sku: {}", sku);
                        return response;
                    case 400:
                        logger.warn("Amazon: Bad request, sku: {}, body: {}", sku, response.body());
                        Webhook.sendMessage(String.format("Amazon: 400 sku: %s, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, response.body(), sku));
                        return null;
                    case 401:
                        refreshToken();
                        continue;
                    case 403:
                        logger.error("Amazon: Forbidden, check permissions. sku: {}, body: {}", sku, response.body());
                        refreshToken();
                        continue;
                    case 404:
                        //logger.warn("Amazon: 404 not found, sku: {}", sku);
                        return response; // returned so caller can handle (e.g. item doesn't exist yet)
                    case 422:
                        logger.error("Amazon: Invalid request data, sku: {}, body: {}", sku, response.body());
                        Webhook.sendMessage(String.format("Amazon: 422 sku: %s, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, response.body(), sku));
                        return null;
                    case 429:
                        try {
                            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                            long waitMs = retryAfter != null
                                ? Long.parseLong(retryAfter) * 1000
                                : (long)(Math.pow(2, i) * 1000);
                            logger.warn("Amazon: Rate limited, waiting {}ms. sku: {}", waitMs, sku);
                            Thread.sleep(waitMs);
                        } catch (Exception ex) {
                            logger.error("Amazon: Sleep failed: {}", ex.getMessage());
                        }
                        continue;
                    default:
                        if (status >= 500) {
                            logger.error("Amazon: Server error, will retry. sku: {}, status: {}, body: {}", sku, status, response.body());
                            double wait = Math.pow(2, i) + Math.random();
                            try {
                                Thread.sleep((long)(wait * 1000));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return null;
                            }
                            continue;
                        }
                        logger.error("Amazon: Unhandled client error, not retrying. sku: {}, status: {}, body: {}", sku, status, response.body());
                        Webhook.sendMessage(String.format("Amazon: Unhandled client error, not retrying. sku: %s, status: %d, body: %s \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, status, response.body(), sku));
                        return null;
                }
            }
        }

        logger.error("Amazon: Failed poison!: sku: {}", sku);
        Webhook.sendMessage(String.format("Amazon: Failed poison!: sku: %s, attempt: %d \nhttps://app.lucasschottler.dev/admin/inventory/%s", sku, max_retries, sku));
        return null;
    }

}
