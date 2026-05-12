package dev.lucasschottler.api.update;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.lucasschottler.marketplaces.Amazon;

@Component
public class CronJob {

    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    private static Amazon amazon = new Amazon();

    @Scheduled(fixedRate = 30000)
    public void pollAmazonOrders() {
        String createdAfter = ZonedDateTime.now(ZoneOffset.UTC)
            .minusHours(24)
            .format(DateTimeFormatter.ISO_INSTANT);
        
        logger.info("Cron: Getting orders created in the last: {}", createdAfter);

        Map<String, Object> orders = amazon.getOrders(createdAfter);

        logger.info("Cron: Orders: {}", orders);
    }
}