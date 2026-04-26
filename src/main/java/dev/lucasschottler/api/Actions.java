package dev.lucasschottler.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import dev.lucasschottler.api.square.Square;
import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.update.Amazon;
import dev.lucasschottler.update.Amazon.AmazonNotFoundException;
import dev.lucasschottler.update.Ebay;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    private final StateService stateService;
    private final Square square;
    private Lakes lakes;
    Amazon amazon = new Amazon();

    public Actions(Databasing db, Lakes lakes, StateService stateService, Square square){
        this.db = db;
        this.stateService = stateService;
        this.square = square;
        this.lakes = lakes;
    }

    public boolean resetItem(String sku){

        Map<String, Object> item = db.getData(sku);

        if(item == null){
            logger.warn("Actions failed to reset sku: {} due to no results", sku);
            return false;
        }

        //logger.info("Actions starting update on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);

        //logger.info("Actions update resolved dbItem for sku: {}", dbItem.sku);

        LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);

        //logger.info("Actions update resolved lakesItem for sku: {}", lakesItem.sku);
        
        db.resetItem(lakesItem);

        return true;
    }

    public DatabaseItem updateItem(String sku){

        //logger.info("Actions: Getting item data for updateItem... {}", sku);
        Map<String, Object> item = db.getData(sku);
        //logger.info("Actions: Item data retrieved for {}", sku);

        if(item == null){
            logger.warn("Actions: failed to update item sku: {} due to no results", sku);
            return null;
        }

        Double bulkSplitPrice = getBulkSplitPrice(sku);

        //logger.info("Actions starting update on item: {}", item);
        DatabaseItem dbItem = new DatabaseItem(item);
        //logger.info("Actions update resolved dbItem for sku: {}", dbItem.sku);

        if(bulkSplitPrice != null && bulkSplitPrice > 0){
            dbItem.setPricingFields(bulkSplitPrice, db);
        }

        Integer lakesid = dbItem.lakesid;
        if(lakesid != null){
            LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);
            //logger.info("Actions update resolved lakesItem for sku: {}", lakesItem.sku);

            dbItem.updateItemUsingLakes(lakesItem, db);
            //logger.info("Actions resolved updateItemUsingLakes on database for sku: {}", dbItem.sku);
        } else {
            List<Map<String,Object>> bom = db.getBom(dbItem.sku);

            if(bom.size() > 0){
                String child_sku = (String) bom.get(0).get("child_sku");

                if(child_sku != null){
                    DatabaseItem dbChildItem = new DatabaseItem(db.getData(child_sku));

                    LakesItem lakesChildItem = lakes.getLakesItem(dbChildItem.lakesid);
                    //logger.info("Actions update resolved lakesItem for sku: {} using dependency lakesid", lakesChildItem.sku);

                    dbItem.updateItemUsingLakes(lakesChildItem, db);
                    //logger.info("Actions resolved updateItemUsingLakes on database for sku: {} using dependency lakesid", dbItem.sku);
                }
            }
        }

        if(dbItem.square_variation_id != null){

            Integer square_quantity = square.getInventoryCountByVariationID(dbItem.square_variation_id);

            //logger.info("Actions: Square quantity found: {}", square_quantity);

            if(square_quantity != null){
                int quantity = square.getInventoryCountByVariationID(dbItem.square_variation_id);
                db.updateCustomQuantity(dbItem.sku, quantity);
            }
        }

        return dbItem;
    }

    public boolean updateAndPushItem(String sku){

        //logger.info("Actions: calling updateItem... {}", sku);
        DatabaseItem dbItem = updateItem(sku);
        //logger.info("Actions: success! Got dbItem {}", sku);

        //logger.info("Actions: Recieved object: {}", dbItem.toString());

        if(dbItem.marketplaces == null || dbItem.marketplaces.equals("")){
            //logger.info("Actions update and push found no marketplaces!, sku: {}", dbItem.sku);
            return true;
        }

        boolean amazonSuccess = false;
        boolean ebaySuccess = false;

        if(dbItem.marketplaces.contains("amazon")){
            try{
                //logger.info("Actions update pushing to amazon, sku: {}", dbItem.sku);
                if(amazon.updateItem(dbItem)){
                    db.updateLastSuccess("amazon", sku);
                    amazonSuccess = true;
                } else {
                    logger.info("Actions: amazon failure to update item, sku: {}", dbItem.sku);
                }
                //logger.info("Actions update FINISHED pushing to amazon, sku: {}", dbItem.sku);
            } catch (AmazonNotFoundException e){
                //logger.info("Actions: Amazon item not found Exception:", e);
            }
            
        }

        if(dbItem.marketplaces.contains("ebay")){

            boolean successOnItem = false;
            boolean successOnOffer = false;

            //logger.info("Actions update updating inventory to ebay, sku: {}", dbItem.sku);

            boolean ebayCreateOrUpdate = dbItem.last_ebay == null 
                ? Ebay.createOrUpdateItem(dbItem) 
                : Ebay.updateItem(dbItem);

            if (ebayCreateOrUpdate){
                
                //logger.info("Actions update SUCCESS updating inventory to ebay, sku: {}", dbItem.sku);
                successOnItem = true;
            } else {
                logger.info("Actions: createOrUpdate FAILURE updating inventory to ebay, sku: {}", dbItem.sku);
            }

            //logger.info("Actions update offer to ebay, sku: {}", dbItem.sku);
            if(Ebay.updateOffer(dbItem)){
                //logger.info("Actions update SUCCESS offer to ebay, sku: {}", dbItem.sku);
                successOnOffer = true;
            } else {
                logger.info("Actions: updateOffer FAILURE updating inventory to ebay, sku: {}", dbItem.sku);
            }

            if(successOnItem && successOnOffer){
                db.updateLastSuccess("ebay", sku);
                ebaySuccess = true;
            } else {
                logger.warn("Actions: Failure to update ebay offer or item, sku: {}", dbItem.sku);
            }   
        }
        
        return ebaySuccess && amazonSuccess;
    }

    public void updateInventory() {
        //logger.info("Actions getting database...");
        List<Map<String, Object>> data = db.queryDatabase(null, 12000, null);
        //logger.info("Actions received database with {} items", data.size());

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
            //logger.info("Actions starting on item: {}", item);
            DatabaseItem dbItem = new DatabaseItem(item);

            LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);
            dbItem.updateItemUsingLakes(lakesItem, db);

            if (dbItem.square_variation_id != null) {
                Integer square_quantity = square.getInventoryCountByVariationID(dbItem.square_variation_id);
                //logger.info("Actions: Square quantity found: {}", square_quantity);
                if (square_quantity != null) {
                    db.updateCustomQuantity(dbItem.sku, square_quantity);
                    dbItem.custom_quantity = square_quantity;
                }
            }

            if (dbItem.marketplaces == null || dbItem.marketplaces.isEmpty()) {
                //logger.info("Actions: No marketplaces found for sku: {}", dbItem.sku);
                return;
            }

            if (dbItem.marketplaces.contains("amazon")) {
                try{
                    if (!amazon.updateItem(dbItem)) {
                        //logger.info("Actions: Failure to update amazon item in process!: {}", dbItem.sku);
                    } else {
                        db.updateLastSuccess("amazon", dbItem.sku);
                    }
                } catch (AmazonNotFoundException e){
                    //logger.info("Actions: Amazon item was not found exception!: {}", e);
                }
            }

            if (dbItem.marketplaces.contains("ebay")) {
                boolean ebaySuccess = dbItem.last_ebay == null
                    ? Ebay.createOrUpdateItem(dbItem)
                    : Ebay.updateItem(dbItem);

                if (ebaySuccess) {
                    if (Ebay.updateOffer(dbItem)) {
                        db.updateLastSuccess("ebay", dbItem.sku);
                    } else {
                        logger.info("Actions: Failure to update ebay offer in process!: {}", dbItem.sku);
                    }
                } else {
                    logger.info("Actions: Failure to update ebay item in process!: {}", dbItem.sku);
                }
            }

            //logger.info("Actions completed processing sku: {}", dbItem.sku);
        } catch (Exception e) {
            logger.error("Actions: Exception processing item raw: {}", item, e);
        }
    }

    public Double getBulkSplitPrice(String sku){
        logger.info("Actions: Testing for Bom relations...");
        List<Map<String,Object>> bom = db.getBom(sku);

        logger.info("Actions - Bom: Got the bom for sku: {}, BOM: {}", sku, bom);

        Double bulkSplitPrice = 0.0;

        if(bom != null && !bom.isEmpty()){

            logger.info("Actions: Found bom relations!");

            for (Map<String,Object> dependency : bom){
                String dependency_sku = (String) dependency.get("child_sku");
                if(dependency_sku != null){

                    logger.info("Actions - Bom: updating item for dependency: {}", dependency_sku);

                    updateItem(dependency_sku);

                    logger.info("Actions - Bom: updated and now fetching data again for dependency: {}", dependency_sku);

                    Map<String, Object> dependency_data;

                    try {
                        dependency_data = db.getData(dependency_sku);
                    } catch (EmptyResultDataAccessException e) {
                        logger.warn("Actions - Bom: dependency not found in database: {}", dependency_sku);
                        continue;
                    }

                    logger.info("Actions - Bom: fetched now parsing prices for dependency: {}", dependency_sku);

                    Double lakes_price = ((Number) dependency_data.get("lakes_price")).doubleValue();
                    Double quantity = ((Number) dependency.get("quantity")).doubleValue();

                    logger.info("Actions - Bom: Prices found: lakes: {}, quantity: {}, dependency: {}", lakes_price, quantity, dependency_sku);

                    if(quantity != null){
                        if(lakes_price != null && lakes_price > 0){
                            bulkSplitPrice += lakes_price * quantity;
                        }

                        logger.info("Actions - Bom: updating bulkSplitPrice: {} for parent: {}", bulkSplitPrice, sku);
                    }
                    
                }
            }
        } else {
            return null;
        }

        logger.info("Actions: final bulkSplitPrice: {}", bulkSplitPrice);

        return bulkSplitPrice;
    }

}
