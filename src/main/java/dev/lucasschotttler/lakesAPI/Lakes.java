package dev.lucasschotttler.lakesAPI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

public class Lakes {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final static String ITEMLINK = "https://swymstore-v3pro-01.swymrelay.com/api/v2/provider/getPlatformProducts?pid=jn9XxHMVJRoc160vy%2BI3OVpfL8Wq3P19N1qklE2GjTk%3D";
    private final static String APILINK = "https://searchserverapi1.com/getresults?api_key=4O3Y4Q0o6o";

    public String getItemAPILink() {
        return ITEMLINK;
    }

    public static String getLakesAPILink() {
        return APILINK;
    }
    
    public static class LakesReturn {
        private List<Item> items;
        private int totalItems;

        public List<Item> getItems() { return items; }
        public void setItems(List<Item> items) { this.items = items; }
        
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
public static class Item {
    private String product_id;
    private String product_code;
    private String title;
    private String description;
    private String upc;
    private String brand;
    private Integer quantity;
    private String sku;
    private String name;
    private Double width;
    private Double length;  // Note: This is called "depth" in the API
    private Double height;
    private Double weight;

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }
    
    public String getProduct_code() { return product_code; }
    public void setProduct_code(String product_code) { this.product_code = product_code; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUpc() { return upc; }
    public void setUpc(String upc) { this.upc = upc; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }
    
    public Double getLength() { return length; }
    public void setLength(Double length) { this.length = length; }
    
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}

private Item getLakesItemWithRetry(String id, int retryCount) {
    final int MAX_RETRIES = 3;
    
    if (retryCount >= MAX_RETRIES) {
        System.err.println("Max retries reached for item: " + id);
        return null;
    }
    
    System.out.println("Attempting Lakes Item: " + id + " (attempt " + (retryCount + 1) + ")");
    
    try {
        String payload = String.format(
            "productids=%%5B%s%%5D&regid=JXNnJGEEgrP63HI0SQEsMlT-lqGvpin-gs-TMt4v7KZNhXb8BXV3AGU9VvweaaoRkXWjtvc25shVAKa5Zb5MaI2GlIuSXzYIpfEQ-l87Y2qaaVGq2dRdEzHBjvkTweZeZjjfPDycP_6LDolPIapsKuHskOVMaGSnI_JsLF20Py8&sessionid=qtwf2nkbfh6bbs8f9c4u8ux434l4vhnlb34okc9a675gabmmigagcv6ojyu8ou04",
            id
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getItemAPILink()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .timeout(java.time.Duration.ofSeconds(10))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode productData = rootNode.get(0).get("productdata");
                
                // Create Item and manually populate fields
                Item item = new Item();
                item.setProduct_id(productData.has("id") ? productData.get("id").asText() : id);
                item.setName(productData.has("name") ? productData.get("name").asText() : null);
                item.setTitle(productData.has("name") ? productData.get("name").asText() : null);
                item.setDescription(productData.has("description") ? productData.get("description").asText() : null);
                item.setProduct_code(productData.has("mpn") ? productData.get("mpn").asText() : null);
                item.setBrand(productData.has("brand_id") ? productData.get("brand_id").asText() : null);
                
                // Get data from variants array
                if (productData.has("variants") && productData.get("variants").isArray() && productData.get("variants").size() > 0) {
                    JsonNode variant = productData.get("variants").get(0);
                    item.setUpc(variant.has("upc") ? variant.get("upc").asText() : null);
                    item.setSku(variant.has("sku") ? variant.get("sku").asText() : null);
                    item.setQuantity(variant.has("inventory_level") ? variant.get("inventory_level").asInt() : 0);
                    
                    // Extract dimensions
                    if (variant.has("width") && !variant.get("width").isNull()) {
                        item.setWidth(Math.round(variant.get("width").asDouble() * 100.0) / 100.0);
                    }
                    if (variant.has("depth") && !variant.get("depth").isNull()) {
                        item.setLength(Math.round(variant.get("depth").asDouble() * 100.0) / 100.0);
                    }
                    if (variant.has("height") && !variant.get("height").isNull()) {
                        item.setHeight(Math.round(variant.get("height").asDouble() * 100.0) / 100.0);
                    }
                    if (variant.has("weight") && !variant.get("weight").isNull()) {
                        item.setWeight(Math.round(variant.get("weight").asDouble() * 100.0) / 100.0);
                    }
                }
                
                return item;
            } else {
                System.err.println("Empty or invalid response array");
                return null;
            }
        } else {
            System.out.println("Non-200 response: " + response.statusCode());
            Thread.sleep(1000);
            return getLakesItemWithRetry(id, retryCount + 1);
        }
        
    } catch (Exception e) {
        System.err.println("Exception fetching item " + id + ": " + e.getClass().getName() + " - " + e.getMessage());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return getLakesItemWithRetry(id, retryCount + 1);
    }
}

public Item getLakesItem(String id) {
    return getLakesItemWithRetry(id, 0);
}
}