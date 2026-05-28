package dev.lucasschottler.marketplaces.ingresTypes;

import lombok.Data;
import java.util.List;

@Data
public class AmazonOrder {
    private String orderId;
    private String createdTime;
    private String lastUpdatedTime;
    private List<OrderAlias> orderAliases;
    private List<String> programs;
    private Fulfillment fulfillment;
    private SalesChannel salesChannel;
    private List<OrderItem> orderItems;

    @Data
    public static class OrderAlias {
        private String aliasId;
        private String aliasType;
    }

    @Data
    public static class Fulfillment {
        private ShipWindow shipByWindow;
        private String fulfilledBy;
        private ShipWindow deliverByWindow;
        private String fulfillmentStatus;
        private String fulfillmentServiceLevel;
    }

    @Data
    public static class ShipWindow {
        private String earliestDateTime;
        private String latestDateTime;
    }

    @Data
    public static class SalesChannel {
        private String marketplaceId;
        private String marketplaceName;
        private String channelName;
    }

    @Data
    public static class OrderItem {
        private Product product;
        private String orderItemId;
        private int quantityOrdered;
        private ItemFulfillment fulfillment;
    }

    @Data
    public static class Product {
        private Condition condition;
        private Price price;
        private String asin;
        private String sellerSku;
        private String title;
    }

    @Data
    public static class Condition {
        private String conditionType;
        private String conditionSubtype;
    }

    @Data
    public static class Price {
        private UnitPrice unitPrice;
        private String priceDesignation;
    }

    @Data
    public static class UnitPrice {
        private String amount;
        private String currencyCode;
    }

    @Data
    public static class ItemFulfillment {
        private int quantityFulfilled;
        private int quantityUnfulfilled;
    }
}