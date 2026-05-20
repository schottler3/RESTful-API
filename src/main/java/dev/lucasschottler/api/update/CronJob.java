package dev.lucasschottler.api.update;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.marketplaces.Amazon;
import dev.lucasschottler.marketplaces.types.AmazonOrder;

@Component
public class CronJob {
    private static final Logger logger = LoggerFactory.getLogger(CronJob.class);
    private final Amazon amazon;
    private  final Actions actions;
    private final Databasing db;


    public CronJob(Amazon amazon, Actions actions, Databasing db) {
        this.amazon = amazon;
        this.actions = actions;
        this.db = db;
    }

    @Scheduled(fixedRate = 900000)
    public void pollAmazonOrders() {
        String createdAfter = ZonedDateTime.now(ZoneOffset.UTC)
            .minusHours(1)
            .format(DateTimeFormatter.ISO_INSTANT);
        
        logger.info("Cron: Getting orders created in the last: {}", createdAfter);

        List<AmazonOrder> orders = amazon.getOrders(createdAfter);

        for(AmazonOrder order: orders){

            String status = order.getFulfillment().getFulfillmentStatus();
            if(status.equals("CANCELLED")){
                actions.cancelOrder(db.updateOrderStatus(order.getOrderId(), status));
            } else {
                actions.updateSquareInventory(db.addAmazonOrder(order));
            }
        }

        //logger.info("Cron: Orders: {}", orders);
    }
}