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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ebay {

    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    
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
        
        // Prepare body
        String requestBody = String.format(
            "grant_type=%s&refresh_token=%s&scope=%s",
            URLEncoder.encode("refresh_token", StandardCharsets.UTF_8),
            URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
            URLEncoder.encode("https://api.ebay.com/oauth/api_scope", StandardCharsets.UTF_8)
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
                String newAccessToken = (String) tokenData.get("access_token");
                
                logger.info("✅ Token refreshed successfully");
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

    public boolean createOrUpdateItem(int id, int fulfillment){

        String TOKEN = System.getenv("EBAY_TOKEN");
        String API = System.getenv("EBAY_INVENTORY_API_END");

        return false;
    }
}
