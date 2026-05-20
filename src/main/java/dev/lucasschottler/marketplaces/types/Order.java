package dev.lucasschottler.marketplaces.types;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

@Data
public class Order {
    String orderId;
    Marketplace marketplace;
    String status;
    List<Item> items;
    Timestamp timePlaced;
    Timestamp createdAt;

    @Data
    public static class Item {
        public String sku;
        public int quantity;
    }
}