package dev.lucasschottler.marketplaces.types;

import lombok.Data;
import java.util.List;

@Data
public class EbayOrderConfirmation {
    private Metadata metadata;
    private Notification notification;

    @Data
    public static class Metadata {
        private String topic;
        private String schemaVersion;
        private boolean deprecated;
    }

    @Data
    public static class Notification {
        private String notificationId;
        private String eventDate;
        private String publishDate;
        private int publishAttemptCount;
        private NotificationData data;
    }

    @Data
    public static class NotificationData {
        private Order order;
    }

    @Data
    public static class Order {
        private String orderId;
        private List<OrderLineItem> orderLineItems;
    }

    @Data
    public static class OrderLineItem {
        private String orderLineItemId;
        private String listingId;
        private int quantity;
    }
}