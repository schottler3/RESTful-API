package dev.lucasschotttler.lakes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LakesItem {
    public int quantity;
    public double price;
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

            JsonNode item = root.has("data") ? root.get("data").get(0) : root;

            this.quantity = item.has("quantity") ? item.get("quantity").asInt() : 0;
            this.price = item.has("price") ? item.get("price").asDouble() : 999.99;
            this.width = item.has("width") ? item.get("width").asDouble() : 0.0;
            this.length = item.has("length") ? item.get("length").asDouble() : 0.0;
            this.height = item.has("height") ? item.get("height").asDouble() : 0.0;
            this.weight = item.has("weight") ? item.get("weight").asDouble() : 0.0;
            this.type = item.has("type") ? item.get("type").asText() : null;
            this.mpn = item.has("mpn") ? item.get("mpn").asText() : null;
            this.title = item.has("title") ? item.get("title").asText() : null;
            this.description = item.has("description") ? item.get("description").asText() : null;
            this.upc = item.has("upc") ? item.get("upc").asText() : null;
            this.sku = item.has("sku") ? item.get("sku").asText() : null;
            this.imageLink = item.has("imageLink") ? item.get("imageLink").asText() : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LakesItem JSON", e);
        }
    }
}