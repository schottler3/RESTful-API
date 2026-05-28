package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;
import java.util.List;

import dev.lucasschottler.marketplaces.ingresTypes.Marketplace;
import lombok.Data;

@Data
public class Order {
    String orderId;
    Marketplace marketplace;
    String status;
    List<Item> items;
    Timestamp createdAt;
    Timestamp updatedAt;

    @Data
    public static class Item {
        public String sku;
        public int quantity;
    }
}