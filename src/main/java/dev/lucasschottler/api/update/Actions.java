package dev.lucasschottler.api.update;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import dev.lucasschottler.api.StateService;
import dev.lucasschottler.api.Webhook;
import dev.lucasschottler.api.square.Square;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.database.queries.BatchQueries;
import dev.lucasschottler.database.queries.BomQueries;
import dev.lucasschottler.database.queries.DatabaseItemQueries;
import dev.lucasschottler.database.queries.ErrorQueries;
import dev.lucasschottler.database.queries.ReportQueries;
import dev.lucasschottler.database.tableData.Bom;
import dev.lucasschottler.database.tableData.DatabaseItem;
import dev.lucasschottler.database.tableData.Order;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.marketplaces.Amazon;
import dev.lucasschottler.marketplaces.Ebay;
import dev.lucasschottler.marketplaces.Amazon.AmazonNotFoundException;
import dev.lucasschottler.marketplaces.ingresTypes.Marketplace;

@Service
public class Actions {
    
    private final Databasing db;
    private final DatabaseItemQueries databaseItemQueries;
    private final BomQueries bomQueries;
    private final ReportQueries reportQueries;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    private final StateService stateService;
    private final Square square;
    private final Ebay ebay;
    private final Lakes lakes;
    private final Amazon amazon;
    private final BatchQueries batchQueries;
    private final ErrorQueries errorQueries;

    private UUID batchId;

    public Actions(
        Databasing db, 
        DatabaseItemQueries databaseItemQueries, 
        BomQueries bomQueries,
        ReportQueries reportQueries,
        Lakes lakes, 
        StateService stateService, 
        Square square, 
        Ebay ebay,
        Amazon amazon, 
        BatchQueries batchQueries,
        ErrorQueries errorQueries
    ){
        this.db = db;
        this.databaseItemQueries = databaseItemQueries;
        this.bomQueries = bomQueries;
        this.reportQueries = reportQueries;
        this.stateService = stateService;
        this.square = square;
        this.lakes = lakes;
        this.ebay = ebay;
        this.amazon = amazon;
        this.batchQueries = batchQueries;
        this.errorQueries = errorQueries;
    }

    public boolean resetItem(String sku){

        DatabaseItem dbItem = databaseItemQueries.getData(sku);

        LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);

        if(lakesItem == null){
            reportQueries.addReportDiscItem(dbItem);
            return false;
        }
        
        databaseItemQueries.resetItem(lakesItem);

        return true;
    }

    public DatabaseItem updateItem(String sku){

        DatabaseItem dbItem = databaseItemQueries.getData(sku);
        Integer lakesid = dbItem.lakesid;

        if(lakesid != null){
            //Update the current data based off of lakes data

            LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);

            if(lakesItem == null){
                reportQueries.addReportDiscItem(dbItem);
                return dbItem;
            }

            dbItem.updateItemUsingLakes(lakesItem, databaseItemQueries);

        } else {
            // if there is no associated lakesid then attempt to use the child for data
            DatabaseItem dbChildItem = dbItem.getParentData(bomQueries, databaseItemQueries);

            if(dbChildItem.lakesid != null){
                LakesItem lakesChildItem = lakes.getLakesItem(dbChildItem.lakesid);

                if(lakesChildItem == null){
                    reportQueries.addReportDiscItem(dbChildItem);
                } else {
                    dbItem.updateItemUsingLakes(lakesChildItem, databaseItemQueries);
                }
            }
        }

        dbItem.updateItem(databaseItemQueries, bomQueries, batchId, this);

        //logger.info("Actions: updateItem has updated this item: {}\n===================\n{}", dbItem.sku, dbItem.toString());

        return dbItem;
    }

    public boolean updateAndPushItem(String sku){

        DatabaseItem dbItem = updateItem(sku);

        if(dbItem.marketplaces == null || dbItem.marketplaces.isBlank()){
            return true;
        }

        logger.info("Actions: Attempting updateAndPush with data: {}", dbItem);

        boolean amazonSuccess = false;
        boolean ebaySuccess = false;

        if(dbItem.marketplaces.contains("amazon")){
            try{
                if(amazon.updateItem(dbItem)){
                    db.updateLastSuccess("amazon", sku);
                    amazonSuccess = true;
                } else {
                    logger.info("Actions: amazon failure to update item, sku: {}", dbItem.sku);
                }
            } catch (AmazonNotFoundException e){

            }
            
        }

        if(dbItem.marketplaces.contains("ebay")){

            //Removed updateItem for verbose "push database" verbage
            boolean successOnItem = ebay.createOrUpdateItem(dbItem);

            boolean successOnOffer = ebay.updateOffer(dbItem);

            if(successOnItem && successOnOffer){
                ebaySuccess = db.updateLastSuccess("ebay", sku);
                ebaySuccess = true;
            }
        }
        
        return ebaySuccess && amazonSuccess;
    }

    public void updateInventory() {
        List<DatabaseItem> data = databaseItemQueries.queryDatabase(null, 12000, null);

        batchId = UUID.randomUUID();
        batchQueries.createBatch(batchId);
        logger.info("Actions: Current batchId: {}", batchId);

        ExecutorService executor = Executors.newFixedThreadPool(12);

        try {
            List<CompletableFuture<Void>> futures = data.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        if ("false".equals(stateService.getState("isUpdating"))) {
                            return;
                        }
                        if(processItem(item)){
                            databaseItemQueries.patchItem(item.sku, "batch_id", batchId);
                        }
                    } catch (Exception e) {
                        logger.error("Actions error processing item: {}, error: {}", item, e.getMessage());
                        errorQueries.addError(batchId, item.sku, e.getMessage());
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

    private boolean processItem(DatabaseItem dbItem) {
        try {
            // if (dbItem.square_variation_id != null) {
            //     Integer square_quantity = square.getInventoryCountByVariationID(dbItem.square_variation_id);
            //     if (square_quantity != null) {
            //         databaseItemQueries.updateCustomQuantity(dbItem.sku, square_quantity);
            //         dbItem.custom_quantity = square_quantity;
            //     }
            // }

            return updateAndPushItem(dbItem.sku);

        } catch (Exception e) {
            logger.error("Actions: Exception processing item raw: {}", dbItem, e);
            return false;
        }
    }

    public boolean updateSquareInventory(String sku, int quantity){

        DatabaseItem dbItem = databaseItemQueries.getData(sku);

        String variationId = dbItem.square_variation_id;

        if(variationId == null || variationId.isBlank()){
            variationId = square.getVariationID(sku);
        }

        if(variationId == null || variationId.isBlank()){
            return false;
        } else {
            databaseItemQueries.patchItem(sku, "square_quantity", quantity);
            return square.updateInventoryCount(variationId, quantity);
        }

    }

    public boolean updateSquareInventory(String orderId) {
        Order order = db.getOrder(orderId);
        for (Order.Item item : order.getItems()) {

            if(order.getMarketplace().equals(Marketplace.AMAZON)){
                Webhook.sendAmazonMessage(String.format("Actions: An Amazon order item has been placed. Sku: %s with Quantity: %d @ Time: %s", item.sku, item.quantity, order.getUpdatedAt().toString()));
            } else {
                Webhook.sendEbayMessage(String.format("Actions: An Ebay order item has been placed. Sku: %s with Quantity: %d @ Time: %s", item.sku, item.quantity, order.getUpdatedAt().toString()));
            }

            DatabaseItem dbItem = databaseItemQueries.getData(item.sku);
            String variationId = dbItem.square_variation_id;

            if (variationId == null || variationId.isBlank()) {
                logger.info("Actions: No variationId was found for this item: {}", item.sku);
                variationId = square.getVariationID(item.sku);
            }

            if (variationId == null || variationId.isBlank()) {
                logger.info("Actions: This item is not in square inventory: {}", item.sku);
                return false;
            }

            if(dbItem.square_quantity != null && dbItem.square_quantity >= 1){

                logger.info("Actions: Found a square_variation_id and is updating db and square!: {}, {}", item.sku, item.quantity);

                int quantity = dbItem.square_quantity - item.quantity;
                if(quantity < 0){
                    quantity = 0;
                }

                if(!databaseItemQueries.patchItem(item.sku, "square_quantity", quantity)){
                    logger.warn("Actions: Failed to update the custom quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                    return false;
                }

                if (!square.updateInventoryCount(variationId, dbItem.square_quantity - quantity)) {
                    logger.warn("Actions: Failed to update the square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                    return false;
                }

                logger.warn("Actions: Sucessfully updated the custom and square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
            }
        }

        return true;
    }

    public boolean cancelOrder(String orderId){
        Order order = db.getOrder(orderId);
        if(order.getStatus().equals("CANCELLED")){

            for (Order.Item item : order.getItems()) {

                if(order.getMarketplace().equals(Marketplace.AMAZON)){
                    Webhook.sendAmazonMessage(String.format("Actions: An Amazon item has been cancelled. Sku: %s with Quantity: %d @ Time: %s", item.sku, item.quantity, order.getUpdatedAt().toString()));
                } else {
                    Webhook.sendEbayMessage(String.format("Actions: An Ebay item has been cancelled. Sku: %s with Quantity: %d @ Time: %s", item.sku, item.quantity, order.getUpdatedAt().toString()));
                }
                
                DatabaseItem dbItem = databaseItemQueries.getData(item.sku);
                String variationId = dbItem.square_variation_id;
                int squareQuantity = square.getInventoryCountByMpn(dbItem.mpn);

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: No variationId was found for this item: {}", item.sku);
                    variationId = square.getVariationID(item.sku);
                }

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: This item is not in square inventory: {}", item.sku);
                    return false;
                }
                
                if (!square.updateInventoryCount(variationId, squareQuantity + item.quantity)) {
                    logger.warn("Actions: Failed to update the square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                    return false;
                }

                if(dbItem.square_variation_id != null && !dbItem.square_variation_id.isBlank()){
                    logger.info("Actions: Found a square_variation_id and is updating db and square!: {}, {}", item.sku, item.quantity);
                    if(!databaseItemQueries.patchItem(item.sku, "custom_quantity", squareQuantity + item.quantity)){
                        logger.warn("Actions: Failed to update the custom quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                        return false;
                    }
                    logger.warn("Actions: Sucessfully updated the custom and square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                }
            }
        }
        return true;
    }
}
