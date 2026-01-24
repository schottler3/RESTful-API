package dev.lucasschotttler.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.lucasschotttler.database.DatabaseItem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

@Service
public class Amazon {

    private final static String SELLER_ID = System.getenv("AMAZON_SELLER_ID");
    private final static String TOKEN = System.getenv("AMAZON_TOKEN");
    private final static String CLIENT_SECRET = System.getenv("AMAZON_CLIENT_SECRET");
    private final static String IDENTIFIER = System.getenv("AMAZON_IDENTIFIER");
    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private final static String ENDPOINT = "https://sellingpartnerapi-na.amazon.com";
    private final static String MARKETPLACE_ID = "ATVPDKIKX0DER";

    
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

    public static boolean updateItem(DatabaseItem dbItem){
    
        String accessToken = Amazon.getAccessToken(dbItem);

        if(accessToken == null || accessToken.equals("")){
            logger.warn("Amazon Access Token Failed, SKU: {}", dbItem.sku);
            return false;
        }

        final String get_url = ENDPOINT + "/listings/2021-08-01/items/" + SELLER_ID + "/" + dbItem.sku + "?marketplaceIds=" + MARKETPLACE_ID + "&issueLocale=en_US&includedData=attributes";

        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(get_url))
            .header("accept", "application/json")
            .header("x-amz-access-token", accessToken)
            .GET()
            .build();

        HttpResponse<String> get_response = doRequest(getRequest, dbItem.sku);

        if(get_response.statusCode() == 404){
            logger.warn("Amazon item not found or doesn't exist. sku: {}", dbItem.sku);
            return false;
        }

        ObjectNode itemData;

        try{
            itemData = (ObjectNode) mapper.readTree(get_response.body());
        } catch (Exception e){
            logger.error("Amazon JSON mapper error on get_response, sku: {}", dbItem.sku);
            return false;
        }

        // Get the productType from the response
        String productType = "PRODUCT"; // default fallback
        if (itemData.has("productType")) {
            productType = itemData.get("productType").asText();
            logger.info("Amazon productType for sku {}: {}", dbItem.sku, productType);
        } else {
            logger.warn("Amazon productType not found in response for sku: {}, using default", dbItem.sku);
        }

        ObjectNode currentAttributes = null;
        if (itemData.has("attributes") && itemData.get("attributes").isObject()) {
            currentAttributes = (ObjectNode) itemData.get("attributes");
        } else {
            logger.error("Amazon itemData missing 'attributes' field, sku: {}", dbItem.sku);
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
            ourPriceValue.put("value_with_tax", dbItem.custom_price != null ? dbItem.custom_price : dbItem.calculated_price);
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
        fulfillment.put("quantity", dbItem.custom_quantity != null ? dbItem.custom_quantity : (int) (dbItem.quantity * .66));
        fulfillment.put("lead_time_to_ship_max_days", dbItem.fulfillment);
        fulfillmentValue.add(fulfillment);

        fulfillmentPatch.set("value", fulfillmentValue);

        patches.add(fulfillmentPatch);
        //#endregion

        // #region Images patch

        if(dbItem.milwaukee_images != null){
            String[] milwaukee_images = dbItem.milwaukee_images.split(",");

            if(milwaukee_images.length > 0){

                if(milwaukee_images.length > 9){
                    milwaukee_images = java.util.Arrays.copyOf(milwaukee_images, 9);
                }

                for (int i = 1; i < milwaukee_images.length; i++) {
                    ObjectNode imagePatch = mapper.createObjectNode();
                    imagePatch.put("op", "replace");
                    imagePatch.put("path", "/attributes/other_product_image_locator_" + i);

                    ArrayNode valueArr = mapper.createArrayNode();
                    ObjectNode valueObj = mapper.createObjectNode();
                    valueObj.put("media_location", milwaukee_images[i]);
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

        logger.info("Amazon PATCH request body, sku: {}, body: {}", dbItem.sku, requestBody.toString());

        final String patch_url = ENDPOINT + "/listings/2021-08-01/items/" + SELLER_ID + "/" + dbItem.sku + "?marketplaceIds=" + MARKETPLACE_ID;

        HttpRequest patchRequest = HttpRequest.newBuilder()
            .uri(URI.create(patch_url))
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .header("x-amz-access-token", accessToken)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<String> patch_response = doRequest(patchRequest, dbItem.sku);

        if(patch_response == null || (patch_response.statusCode() != 200 && patch_response.statusCode() != 204)) {
            logger.error("Amazon PATCH failed, sku: {}, status: {}", dbItem.sku, 
                patch_response != null ? patch_response.statusCode() : "null");
            return false;
        }

        logger.info("Amazon PATCH finished, sku: {}", dbItem.sku);

        return true;
    }

    private static String getAccessToken(DatabaseItem dbItem){
        try{
            // Build URL-encoded form data
            String formData = "grant_type=refresh_token" +
                "&refresh_token=" + TOKEN +
                "&client_id=" + IDENTIFIER +
                "&client_secret=" + CLIENT_SECRET;
            
            // Build HTTP request with correct content type
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.amazon.com/auth/o2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

            HttpResponse<String> access_token_response = Amazon.doRequest(request, dbItem.sku);

            if(access_token_response == null || access_token_response.statusCode() != 200){
                logger.error("Amazon Returned no Refresh Token. Status: {}, Sku: {}", 
                    access_token_response != null ? access_token_response.statusCode() : "null", dbItem.sku);
                if(access_token_response != null) {
                    logger.error("Response body: {}", access_token_response.body());
                }
                return "";
            }

            try {
                ObjectNode responseJson = (ObjectNode) mapper.readTree(access_token_response.body());
                // Changed from "access_code" to "access_token"
                return responseJson.has("access_token") ? responseJson.get("access_token").asText() : "";
            } catch (Exception e) {
                logger.error("Failed to parse Amazon token response JSON: {}", e.getMessage());
                return "";
            }

        } catch (Exception e){
            logger.error("Amazon refresh_token FAILED. sku: {}, error: {}", dbItem.sku, e.getMessage());
            return "";
        }
    }

    private static HttpResponse<String> doRequest(HttpRequest request, String sku){

        int max_retries = 3;

        for (int i = 0; i < max_retries; i++) {

            logger.info("Amazon Request ATTEMPT: sku: {}, attempt: {}", sku, i + 1);

            HttpResponse<String> response;

            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("Amazon Request FINISHED: sku: {}", sku);
            } catch (Exception e) {
                logger.error("Amazon Request EXCEPTION: sku: {}, attempt: {}, error: {}", sku, i + 1, e.getMessage());
                continue;
            }

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                logger.info("Amazon Request SUCCESS: sku: {}", sku);
                return response;
            } else {
                logger.error("Amazon Request ERROR: sku: {}, status: {}, attempt: {}, body: {}", 
                    sku, response.statusCode(), i + 1, response.body());
                
                // Don't retry on client errors (4xx)
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    logger.error("Amazon Request Client error - not retrying: sku: {}, status: {}", sku, response.statusCode());
                    return response;
                }
                
                double wait = Math.pow(2, i) + Math.random();
                try {
                    Thread.sleep((long)(wait * 1000));
                } catch (InterruptedException ie) {
                    logger.error("Amazon Request Sleep interrupted: sku: {}, attempt: {}", sku, i + 1);
                    Thread.currentThread().interrupt();
                    return response;
                }
            }
        }
        return null;
    }

}
