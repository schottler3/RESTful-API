package dev.lucasschottler.api.square;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Square {

    private final String personalAccessToken = System.getenv("SQUARE_PERSONAL_ACCESS_TOKEN");
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(Square.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public Integer getInventoryCountByMpn(String mpn) {

        String variationId;

        try {
            variationId = getVariationID(mpn);
        } catch (Exception e) {
            logger.error("Square threw an error trying to get the variation ID! mpn: {}", mpn, e);
            return null;
        }

        if (variationId == null) {
            //logger.info("Square was unable to return the variation ID! mpn: {}", mpn);
            return null;
        }

        JsonNode inventoryObject;

        try {
            inventoryObject = getInventoryObject(variationId);
        } catch (Exception e) {
            logger.error("Square threw an exception getting the inventory object for mpn: {}", mpn, e);
            return null;
        }

        return extractQuantityFromInventoryObject(inventoryObject, mpn);
    }

    public Integer getInventoryCountByVariationID(String variationId) {

        JsonNode inventoryObject;

        try {
            inventoryObject = getInventoryObject(variationId);
        } catch (Exception e) {
            logger.error("Square threw an exception getting the inventory object for variationId: {}", variationId, e);
            return null;
        }

        return extractQuantityFromInventoryObject(inventoryObject, variationId);
    }

    private Integer extractQuantityFromInventoryObject(JsonNode inventoryObject, String identifier) {
        if (inventoryObject == null) {
            //logger.info("Square couldn't get the inventory object for: {}", identifier);
            return null;
        }

        JsonNode counts = inventoryObject.path("counts");

        if (counts.isEmpty()) {
            //logger.info("Square returned no counts for: {}", identifier);
            return null;
        }

        Integer inventoryCount = Integer.parseInt(counts.get(0).path("quantity").asText());

        //logger.info("Square found quantity {} for {}", inventoryCount, identifier);

        return inventoryCount;
    }

    private JsonNode getInventoryObject(String variationId) throws IOException, InterruptedException {
        HttpRequest inventoryRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://connect.squareup.com/v2/inventory/" + variationId))
            .header("Authorization", "Bearer " + personalAccessToken)
            .header("Square-Version", "2026-01-22")
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> inventoryResponse = httpClient.send(inventoryRequest, HttpResponse.BodyHandlers.ofString());

        return mapper.readTree(inventoryResponse.body());
    }

    public String getVariationID(String mpn) throws IOException, InterruptedException {
        //logger.info("Square received request for mpn: {}", mpn);

        String searchBody = """
            {
                "object_types": ["ITEM_VARIATION"],
                "query": {
                    "exact_query": {
                        "attribute_name": "sku",
                        "attribute_value": "%s"
                    }
                }
            }
            """.formatted(mpn);

        HttpRequest searchRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://connect.squareup.com/v2/catalog/search"))
            .header("Authorization", "Bearer " + personalAccessToken)
            .header("Square-Version", "2026-01-22")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(searchBody))
            .build();

        //logger.info("Square is sending request for inventory object: {}", searchBody);

        HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

        if (searchResponse.statusCode() < 200 || searchResponse.statusCode() >= 300) {
            logger.error("Square API error: {} - {}", searchResponse.statusCode(), searchResponse.body());
            throw new IOException("Square API request failed with status " + searchResponse.statusCode());
        }

        JsonNode searchJson = mapper.readTree(searchResponse.body());
        JsonNode objects = searchJson.path("objects");

        if (objects.isEmpty()) {
           // logger.info("Square found no SKU: {}", mpn);
            return null;
        }

        //logger.info("Square found sku with object: {}", objects.asText());

        for (JsonNode obj : objects) {
            JsonNode variationData = obj.path("item_variation_data");
            if (variationData.path("track_inventory").asBoolean(false)) {
                String id = obj.path("id").asText();
                logger.info("Square selected trackable variation {} for mpn: {}", id, mpn);
                return id;
            }
        }

        logger.warn("Square found no inventory-tracked variation for mpn: {}", mpn);
        return null;
    }
}