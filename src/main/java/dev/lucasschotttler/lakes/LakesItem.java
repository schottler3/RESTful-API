package dev.lucasschotttler.lakes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LakesItem {
    public int productId;
    public String productCode;
    public int quantity;
    public double price;
    public double regularPrice;
    public double salePrice;
    public double width;
    public double length;
    public double height;
    public double weight;
    public String type;
    public String mpn;
    public String title;
    public String description;
    public String upc;
    public String sku;
    public String imageLink;

    public LakesItem(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode firstElement = root.isArray() && root.size() > 0 ? root.get(0) : root;
            JsonNode productData = firstElement.get("productdata");

            // Map top-level fields
            this.productId = productData.has("id") ? productData.get("id").asInt() : 0;
            this.productCode = productData.has("product_code") ? productData.get("product_code").asText() : "";
            this.quantity = productData.has("inventory_level") ? productData.get("inventory_level").asInt() : 0;
            this.price = productData.has("price") ? productData.get("price").asDouble() : 0.0;
            this.regularPrice = productData.has("retail_price") ? productData.get("retail_price").asDouble() : 0.0;
            this.salePrice = productData.has("sale_price") ? productData.get("sale_price").asDouble() : 0.0;
            this.width = productData.has("width") ? productData.get("width").asDouble() : 0.0;
            this.length = productData.has("depth") ? productData.get("depth").asDouble() : 0.0;
            this.height = productData.has("height") ? productData.get("height").asDouble() : 0.0;
            this.weight = productData.has("weight") ? productData.get("weight").asDouble() : 0.0;
            this.type = productData.has("type") ? productData.get("type").asText() : "";
            this.mpn = productData.has("mpn") ? productData.get("mpn").asText() : "";
            this.title = productData.has("name") ? productData.get("name").asText() : "";
            this.description = productData.has("description") ? productData.get("description").asText() : "";
            this.upc = productData.has("upc") ? productData.get("upc").asText() : "";
            this.sku = productData.has("sku") ? productData.get("sku").asText() : "";

            // Handle images
            JsonNode images = productData.get("images");
            if (images != null && images.isArray() && images.size() > 0) {
                JsonNode image = images.get(0);
                this.imageLink = image.has("url_zoom") ? image.get("url_zoom").asText() : "";
            }

        } catch (Exception e) {
            // Log the error instead of throwing an exception
            System.err.println("Failed to parse LakesItem JSON: " + e.getMessage());
        }
    }
}