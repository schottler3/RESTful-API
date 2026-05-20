package dev.lucasschottler.api.update;

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

import com.fasterxml.jackson.databind.JsonNode;

import dev.lucasschottler.api.StateService;
import dev.lucasschottler.api.square.Square;
import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.marketplaces.Amazon;
import dev.lucasschottler.marketplaces.Ebay;
import dev.lucasschottler.marketplaces.types.Order;
import dev.lucasschottler.marketplaces.Amazon.AmazonNotFoundException;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);
    private final StateService stateService;
    private final Square square;
    private final Ebay ebay;
    private Lakes lakes;
    private Amazon amazon = new Amazon();

    public Actions(Databasing db, Lakes lakes, StateService stateService, Square square, Ebay ebay){
        this.db = db;
        this.stateService = stateService;
        this.square = square;
        this.lakes = lakes;
        this.ebay = ebay;
    }

    public boolean resetItem(String sku){

        Map<String, Object> item = db.getData(sku);

        if(item == null){
            logger.warn("Actions failed to reset sku: {} due to no results", sku);
            return false;
        }

        DatabaseItem dbItem = new DatabaseItem(item);

        LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);

        if(lakesItem == null){
            db.addReportDiscItem(dbItem);
            return false;
        }
        
        db.resetItem(lakesItem);

        return true;
    }

    public DatabaseItem updateItem(String sku){

        Map<String, Object> item = db.getData(sku);

        if(item == null){
            logger.warn("Actions: failed to update item sku: {} No results!", sku);
            return null;
        }

        DatabaseItem dbItem = new DatabaseItem(item); 
        
        List<Map<String,Object>> bom = db.getBom(dbItem.sku);
        Double bulkSplitPrice = getBulkSplitPrice(bom);

        if(bulkSplitPrice != null && bulkSplitPrice > 0){
            dbItem.setPricingFields(bulkSplitPrice, db);
        }

        Integer lakesid = dbItem.lakesid;

        if(lakesid != null){
            //Update the current data based off of lakes data

            LakesItem lakesItem = lakes.getLakesItem(dbItem.lakesid);

            if(lakesItem == null){
                db.addReportDiscItem(dbItem);
                return dbItem;
            }

            dbItem.updateItemUsingLakes(lakesItem, db);

        } else {
            // if there is no associated lakesid then attempt to use the child for data

            if(bom != null && bom.size() > 0){
                String child_sku = (String) bom.get(0).get("child_sku");

                if(child_sku != null){
                    DatabaseItem dbChildItem = new DatabaseItem(db.getData(child_sku));

                    if(dbChildItem.lakesid != null){
                        LakesItem lakesChildItem = lakes.getLakesItem(dbChildItem.lakesid);

                        if(lakesChildItem == null){
                            db.addReportDiscItem(dbItem);
                        } else {
                            dbItem.updateItemUsingLakes(lakesChildItem, db);
                        }

                    }
                }
            }
        }

        dbItem.updateItem(db);

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
        List<Map<String, Object>> data = db.queryDatabase(null, 12000, null);

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
            DatabaseItem dbItem = new DatabaseItem(item);

            if (dbItem.square_variation_id != null) {
                Integer square_quantity = square.getInventoryCountByVariationID(dbItem.square_variation_id);
                if (square_quantity != null) {
                    db.updateCustomQuantity(dbItem.sku, square_quantity);
                    dbItem.custom_quantity = square_quantity;
                }
            }

            updateAndPushItem(dbItem.sku);

        } catch (Exception e) {
            logger.error("Actions: Exception processing item raw: {}", item, e);
        }
    }

    public Double getBulkSplitPrice(List<Map<String,Object>> bom ){
        Double bulkSplitPrice = 0.0;

        if(bom != null && !bom.isEmpty()){

            for (Map<String,Object> dependency : bom){
                String dependency_sku = (String) dependency.get("child_sku");
                if(dependency_sku != null){
                    updateItem(dependency_sku);

                    Map<String, Object> dependency_data;

                    try {
                        dependency_data = db.getData(dependency_sku);
                    } catch (EmptyResultDataAccessException e) {
                        logger.warn("Actions - Bom: dependency not found in database: {}", dependency_sku);
                        continue;
                    }

                    Double lakes_price = ((Number) dependency_data.get("lakes_price")).doubleValue();
                    Double quantity = ((Number) dependency.get("quantity")).doubleValue();

                    if(quantity != null){
                        if(lakes_price != null && lakes_price > 0){
                            bulkSplitPrice += lakes_price * quantity;
                        }
                    }
                }
            }
        } else {
            return null;
        }

        return bulkSplitPrice;
    }

    public boolean updateSquareInventory(String sku, int quantity){

        Map<String, Object> data = db.getData(sku);

        if(data == null){
            return true;
        }

        DatabaseItem dbItem = new DatabaseItem(data);

        String variationId = dbItem.square_variation_id;

        if(variationId == null || variationId.isBlank()){
            variationId = square.getVariationID(sku);
        }

        if(variationId == null || variationId.isBlank()){
            return false;
        } else {
            db.patchItem(sku, "custom_quantity", quantity);
            return square.updateInventoryCount(variationId, quantity);
        }

    }

    public boolean updateSquareInventory(String orderId) {

        Order order = db.getOrder(orderId);
        if(order.getStatus().equals("UNSHIPPED")){

            for (Order.Item item : order.getItems()) {

                Map<String, Object> data = db.getData(item.sku);
                if (data == null) {
                    logger.warn("Actions: The dbItem was not found from sku: {}", item.sku);
                    return false;
                }

                DatabaseItem dbItem = new DatabaseItem(data);
                String variationId = dbItem.square_variation_id;

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: No variationId was found for this item: {}", item.sku);
                    variationId = square.getVariationID(item.sku);
                }

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: This item is not in square inventory: {}", item.sku);
                    return false;
                }

                if(dbItem.custom_quantity != null && dbItem.custom_quantity >= 1){

                    logger.info("Actions: Found a square_variation_id and is updating db and square!: {}, {}", item.sku, item.quantity);

                    int quantity = dbItem.custom_quantity - item.quantity;
                    if(quantity < 0){
                        quantity = 0;
                    }

                    if(!db.patchItem(item.sku, "custom_quantity", quantity)){
                        logger.warn("Actions: Failed to update the custom quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                        return false;
                    }

                    if (!square.updateInventoryCount(variationId, dbItem.custom_quantity - quantity)) {
                        logger.warn("Actions: Failed to update the square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                        return false;
                    }

                    logger.warn("Actions: Sucessfully updated the custom and square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                }
            }
        }

        return true;
    }

    public boolean cancelOrder(String orderId){
        Order order = db.getOrder(orderId);
        if(order.getStatus().equals("CANCELLED")){

            for (Order.Item item : order.getItems()) {

                Map<String, Object> data = db.getData(item.sku);
                if (data == null) {
                    logger.warn("Actions: The dbItem was not found from sku: {}", item.sku);
                    return false;
                }

                DatabaseItem dbItem = new DatabaseItem(data);
                String variationId = dbItem.square_variation_id;

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: No variationId was found for this item: {}", item.sku);
                    variationId = square.getVariationID(item.sku);
                }

                if (variationId == null || variationId.isBlank()) {
                    logger.info("Actions: This item is not in square inventory: {}", item.sku);
                    return false;
                }

                if(dbItem.custom_quantity != null){

                    logger.info("Actions: Found a square_variation_id and is updating db and square!: {}, {}", item.sku, item.quantity);

                    if(!db.patchItem(item.sku, "custom_quantity", dbItem.custom_quantity + item.quantity)){
                        logger.warn("Actions: Failed to update the custom quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                        return false;
                    }

                    if (!square.updateInventoryCount(variationId, dbItem.custom_quantity + item.quantity)) {
                        logger.warn("Actions: Failed to update the square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                        return false;
                    }

                    logger.warn("Actions: Sucessfully updated the custom and square quantity for sku: {} with quantity: {}", item.sku, item.quantity);
                }
            }
        }
        return true;
    }
}
