package dev.lucasschotttler.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dev.lucasschotttler.database.Databasing;
import dev.lucasschotttler.lakes.Lakes;
import dev.lucasschotttler.lakes.LakesItem;
import dev.lucasschotttler.update.Amazon;
import dev.lucasschotttler.update.Ebay;
import dev.lucasschotttler.database.DatabaseItem;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    Amazon amazon = new Amazon();

    public Actions(Databasing db, Lakes lakes){
        this.db = db;
    }

    public DatabaseItem resetItem(int lakesid){

        Map<String, Object> item = db.getData(lakesid, 1);

        if(item == null){
            logger.warn("Actions failed to reset lakesid: {} due to no results", lakesid);
            return null;
        }

        logger.info("Actions starting reset on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);

        logger.info("Actions reset resolved dbItem for sku: {}", dbItem.sku);

        LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);

        logger.info("Actions reset resolved lakesItem for sku: {}", lakesItem.sku);
        
        db.resetItem(lakesItem);

        logger.info("Actions reset resolved resetItem on database for sku: {}", dbItem.sku);

        return dbItem;
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

        return true;
    }

    public void updateInventory(){
        logger.info("Actions getting database...");
        List<Map<String, Object>> data = db.queryDatabase("", 10000);
        logger.info("Actions received database with {} items", data.size());

        if(data.isEmpty()){
            logger.warn("Actions database data is empty!");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(12);
        
        List<CompletableFuture<Void>> futures = data.stream()
            .map(item -> CompletableFuture.runAsync(() -> {
                try {
                    processItem(item);
                } catch (Exception e) {
                    logger.error("Actions error processing item: {}, error: {}", item, e.getMessage());
                }
            }, executor))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        executor.shutdown();
        logger.info("Actions all items processed");
    }

    private void processItem(Map<String, Object> item) {
        logger.info("Actions starting on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);

        LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);
        
        dbItem.updateItem(lakesItem, db);

        amazon.updateItem(dbItem);

        Ebay.updateItem(dbItem);

        logger.info("Actions completed processing sku: {}", dbItem.sku);
    }

}
