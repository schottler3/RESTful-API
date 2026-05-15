package dev.lucasschottler.marketplaces.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.marketplaces.types.AmazonOrder;
import dev.lucasschottler.marketplaces.types.AmazonOrdersResponse;

public class JsonToData{

    private static ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LoggerFactory.getLogger(JsonToData.class);

    public static List<AmazonOrder> parseAmazonOrders(String rawJson){
        try{
            AmazonOrdersResponse response = mapper.readValue(rawJson, AmazonOrdersResponse.class);

            List<AmazonOrder> orders = response.getOrders();

            // Iterate all orders
            for (AmazonOrder order : orders) {
                System.out.println(order.getOrderId());
                System.out.println(order.getFulfillment().getFulfillmentStatus());

                for (AmazonOrder.OrderItem item : order.getOrderItems()) {
                    System.out.println(item.getProduct().getTitle());
                    System.out.println(item.getQuantityOrdered());
                }
            }

            return orders;

        } catch (Exception e){
            logger.error("Failed to parse json from amazon orders! {}", rawJson);
            Webhook.sendMessage("JsonParser: Failed to parse json from Amazon orders!");
            return List.of();
        }
    }


}