package dev.lucasschottler.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.update.Amazon;
import dev.lucasschottler.update.Ebay;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    private final StateService stateService;
    Amazon amazon = new Amazon();

    public Actions(Databasing db, Lakes lakes, StateService stateService){
        this.db = db;
        this.stateService = stateService;
    }

    public boolean resetItem(int lakesid){

        Map<String, Object> item = db.getData(lakesid, 1);

        if(item == null){
            logger.warn("Actions failed to reset lakesid: {} due to no results", lakesid);
            return false;
        }

        logger.info("Actions starting update on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);

        logger.info("Actions update resolved dbItem for sku: {}", dbItem.sku);

        LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);

        logger.info("Actions update resolved lakesItem for sku: {}", lakesItem.sku);
        
        db.resetItem(lakesItem);

        logger.info("Actions reset resolved resetItem on database for sku: {}", dbItem.sku);

        logger.info("Actions update pushing to amazon, sku: ()", dbItem.sku);
        amazon.updateItem(dbItem);
        logger.info("Actions update FINISHED pushing to amazon, sku: ()", dbItem.sku);

        logger.info("Actions update updating inventory to ebay, sku: ()", dbItem.sku);
        Ebay.createOrUpdateItem(dbItem);
        logger.info("Actions update FINISHED updating inventory to ebay, sku: ()", dbItem.sku);

        logger.info("Actions update offer to ebay, sku: ()", dbItem.sku);
        Ebay.updateOffer(dbItem);
        logger.info("Actions update FINISHED offer to ebay, sku: ()", dbItem.sku);

        return true;
    }

    public boolean updateItem(int lakesid){

        Map<String, Object> item = db.getData(lakesid, 1);

        if(item == null){
            logger.warn("Actions failed to update lakesid: {} due to no results", lakesid);
            return false;
        }

        logger.info("Actions starting update on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);
        logger.info("Actions update resolved dbItem for sku: {}", dbItem.sku);

        LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);
        logger.info("Actions update resolved lakesItem for sku: {}", lakesItem.sku);

        dbItem.updateItem(lakesItem, db);
        logger.info("Actions reset resolved updateItem on database for sku: {}", dbItem.sku);

        logger.info("Actions update pushing to amazon, sku: ()", dbItem.sku);
        amazon.updateItem(dbItem);
        logger.info("Actions update FINISHED pushing to amazon, sku: ()", dbItem.sku);

        logger.info("Actions update updating inventory to ebay, sku: ()", dbItem.sku);
        Ebay.createOrUpdateItem(dbItem);
        logger.info("Actions update FINISHED updating inventory to ebay, sku: ()", dbItem.sku);

        logger.info("Actions update offer to ebay, sku: ()", dbItem.sku);
        Ebay.updateOffer(dbItem);
        logger.info("Actions update FINISHED offer to ebay, sku: ()", dbItem.sku);

        return true;
    }

    public void updateInventory() {
        logger.info("Actions getting database...");
        List<Map<String, Object>> data = db.queryDatabase(null, 12000, null);
        logger.info("Actions received database with {} items", data.size());

        if (data.isEmpty()) {
            logger.warn("Actions database data is empty!");
            stateService.setState("isUpdating", "false");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(12);

        try {
            List<CompletableFuture<Void>> futures = data.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        if ("false".equals(stateService.getState("isUpdating"))) {
                            logger.info("Actions updateInventory cancelled, stopping early");
                            return;
                        }
                        processItem(item);
                    } catch (Exception e) {
                        logger.error("Actions error processing item: {}, error: {}", item, e.getMessage());
                    }
                }, executor))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
            stateService.setState("isUpdating", "false");
            logger.info("Actions all items processed");
        }
    }

    private void processItem(Map<String, Object> item) {
        try {
            logger.info("Actions starting on item: {}", item);
            DatabaseItem dbItem = new DatabaseItem(item);

            LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);

            dbItem.updateItem(lakesItem, db);

            amazon.updateItem(dbItem);

            Ebay.updateItem(dbItem);

            logger.info("Actions completed processing sku: {}", dbItem.sku);
        } catch (Exception e) {
            logger.error("Error processing item: {}", item, e);
        }
    }

}
