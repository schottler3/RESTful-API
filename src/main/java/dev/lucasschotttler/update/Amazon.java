package dev.lucasschotttler.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.lucasschotttler.database.DatabaseItem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;

@Service
public class Amazon {

    private final String SELLER_ID = System.getenv("AMAZON_SELLER_ID");
    private final String TOKEN = System.getenv("AMAZON_TOKEN");
    private final String CLIENT_SECRET = System.getenv("AMAZON_CLIENT_SECRET");
    private final String IDENTIFIER = System.getenv("AMAZON_IDENTIFIER");
    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    private static final Amazon amazonService = new Amazon();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String ENDPOINT = "https://sellingpartnerapi-na.amazon.com";
    private final String MARKETPLACE_ID = "ATVPDKIKX0DER";
    
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

    public static boolean patchAmazonItem(DatabaseItem dbItem){
        


        return true;
    }

    private String getAccessToken(DatabaseItem dbItem){

        try{
            ObjectNode tokenRequest = mapper.createObjectNode();
            ObjectNode data = tokenRequest.putObject("data");

            data.put("grant_type", "refresh_token");
            data.put("refresh_token", TOKEN);
            data.put("client_id", IDENTIFIER);
            data.put("client_secret", CLIENT_SECRET);

            String jsonBody;
            try {
                jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tokenRequest);
            } catch (Exception e) {
                logger.error("Failed to serialize JSON on Amazon Refresh error: {}", e.getMessage());
                return "";
            }
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.amazon.com/auth/o2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            if(!amazonService.doRequest(dbItem, request)) {
                logger.error("Amazon refresh_token POST FAILED. sku: {}", dbItem.sku);
            }

            } catch (Exception e){
                logger.error("Amazon refresh_token FAILED. sku: {}", dbItem.sku);
                return "";
            }

    }

    private boolean doRequest(DatabaseItem dbItem, HttpRequest request){

        return true;
    }

}
