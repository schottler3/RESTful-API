package dev.lucasschottler.marketplaces.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.marketplaces.ingresTypes.AmazonOrder;
import dev.lucasschottler.marketplaces.ingresTypes.AmazonOrdersResponse;
import dev.lucasschottler.marketplaces.ingresTypes.EbayOffer;
import dev.lucasschottler.marketplaces.ingresTypes.EbayOrderConfirmation;
import dev.lucasschottler.marketplaces.ingresTypes.EbayOffer.EbayOffersResponse;

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

    public static EbayOrderConfirmation parseEbayOrderConfirmation(String rawJson) {
        try {
            EbayOrderConfirmation confirmation = mapper.readValue(rawJson, EbayOrderConfirmation.class);
 
            return confirmation;
 
        } catch (Exception e) {
            logger.error("Failed to parse json from eBay order confirmation! {}", rawJson);
            Webhook.sendMessage("JsonParser: Failed to parse json from eBay order confirmation!");
            return null;
        }
    }

    public static EbayOffer parseEbayOffer(String rawJson) {
        try {
            EbayOffer offer = mapper.readValue(rawJson, EbayOffer.class);
 
            System.out.println(offer.getOfferId());
            System.out.println(offer.getSku());
            System.out.println(offer.getStatus());
            System.out.println(offer.getListing().getListingStatus());
            System.out.println(offer.getPricingSummary().getPrice().getValue());
 
            return offer;
 
        } catch (Exception e) {
            logger.error("Failed to parse json from eBay offer! {}", rawJson);
            Webhook.sendMessage("JsonParser: Failed to parse json from eBay offer!");
            return null;
        }
    }

    public static List<EbayOffer> parseEbayOffers(String rawJson) {
        try {
            EbayOffersResponse response = mapper.readValue(rawJson, EbayOffersResponse.class);
            return response.getOffers() != null ? response.getOffers() : List.of();
        } catch (Exception e) {
            logger.error("Failed to parse json from eBay offers! {}", rawJson);
            Webhook.sendMessage("JsonParser: Failed to parse json from eBay offers!");
            return List.of();
        }
    }


}