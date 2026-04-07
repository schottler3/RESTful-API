package dev.lucasschottler.api.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.api.Actions;
import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.update.Amazon;
import dev.lucasschottler.api.StateService;
import dev.lucasschottler.api.square.Square;

@RestController
@RequestMapping("/superior/data")
public class DataController {

    private final Databasing db;
    private final Actions actions;
    private final StateService stateService;
    private final Square square;
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);
    private static final String IS_UPDATING_KEY = "isUpdating";

    public DataController(Databasing db, Actions actions, StateService stateService, Square square) {
        this.db = db;
        this.actions = actions;
        this.stateService = stateService;
        this.square = square;
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<?> dataRoot(
        @RequestParam(required = false) String limit,
        @RequestParam(required = false) String keywords,
        @RequestParam(required = false) String time
    ) {
        logger.info("Received request -  Limit: " + limit + ", Keywords: " + keywords);

        int resultLimit = 20;
        
        if (limit != null && !limit.trim().isEmpty()) {
            if (limit.equalsIgnoreCase("All")) {
                resultLimit = Integer.MAX_VALUE;
            } else {
                try {
                    resultLimit = Integer.parseInt(limit);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid limit parameter: {}", limit);
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid limit parameter. Must be a number or 'All'"));
                }
            }
        }

        List<java.util.Map<String, Object>> results = null;

        try {
            results = db.queryDatabase(keywords, resultLimit, time);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: " + e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @PatchMapping({"", "/"})
    public ResponseEntity<?> patchRoot(@RequestBody(required = true) List<Map<String, Object>> requestBody) {

        if (requestBody == null || requestBody.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body is missing or empty"));
        }

        logger.info("Received PATCH request with {} changes", requestBody.size());

        List<Map<String, Object>> failures = new java.util.ArrayList<>();

        try {
            // Process each change
            for (int i = 0; i < requestBody.size(); i++) {
                Map<String, Object> change = requestBody.get(i);
                String sku = (String) change.get("sku");
                String attribute = (String) change.get("attribute");
                String newValue = (String) change.get("new");

                if (sku == null || attribute == null || newValue == null) {
                    logger.warn("Missing required fields in patch: " + i);
                    failures.add(change);
                    continue;
                }

                if (attribute.equals("custom_price")){
                    Double toUsePrice = Double.parseDouble(newValue);
                    if(toUsePrice < 0){
                        logger.error("Failure on Change: sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                        failures.add(change);
                        continue;
                    }
                    else if(toUsePrice == null || toUsePrice == 0){
                        toUsePrice = new DatabaseItem(db.getData(sku)).lakes_price;
                    }

                    if(toUsePrice == null){
                        toUsePrice = -1.0;
                    }

                    HashMap<String,Double> amazonPrices = Amazon.getPrices(toUsePrice);

                    double minimum_price = amazonPrices.get("minimum_price");
                    double calculated_price = amazonPrices.get("middle_price");
                    double maximum_price = amazonPrices.get("maximum_price");

                    logger.info("Data: Updating prices, minimum: {}, calculated: {}, maximum: {}", minimum_price, calculated_price, maximum_price);

                    db.patchItem(sku, "minimum_price", String.valueOf(minimum_price));
                    db.patchItem(sku, "calculated_price", String.valueOf(calculated_price));
                    db.patchItem(sku, "maximum_price", String.valueOf(maximum_price));
                }

                if (attribute.equals("fulfillment") && Integer.parseInt(newValue) <= 0){
                    logger.error("Failure on Change: sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                    failures.add(change);
                    continue;
                }

                if(attribute.equals("barcode_title") && newValue.length() > 36){
                    logger.error("Failure on Change: sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                    failures.add(change);
                    continue;
                }
                
                logger.info("Attempt on Change - sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                if(db.patchItem(sku, attribute, newValue)){
                    logger.info("Success on Change: sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                    actions.updateItem(sku);
                }
                else{
                    logger.error("Failure on Change: sku: {}, attribute: {}, new: {}", sku, attribute, newValue);
                    failures.add(change);
                }
            }
            
            if(failures.isEmpty()){
                return ResponseEntity.ok(Map.of("message", "All patches processed successfully"));
            }
            else{
                return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(failures);
            }
        } catch (Exception e) {
            logger.error("Error processing PATCH request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Internal server error\"}");
        }
    }

    @GetMapping("/item/{sku}")
    public Map<String, Object> getItem(@PathVariable String sku) {
        return db.getData(sku);
    }

    @PutMapping("/reset/{sku}")
    public ResponseEntity<?> resetItem(@PathVariable String sku) {
        try {
            boolean success = actions.resetItem(sku);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Failed to reset item - item not found or reset failed"));
            }
            
            Map<String, Object> updatedItem = db.getData(sku);
            
            if (updatedItem == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Item reset but failed to retrieve updated data"));
            }
            
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Error resetting item {}: {}", sku, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }

    @PostMapping("/item")
    public ResponseEntity<String> addNewItem(@RequestBody(required = true) Map<String, String> requestBody) {

        String sku = requestBody.get("sku");
        String marketplaces = requestBody.get("marketplaces");

        logger.info("Data: Received request to add new sku: {}", sku);

        if(db.createItem(sku, marketplaces)){
            logger.info("Data: Successfully added new sku: {}", sku);
            return ResponseEntity.ok("success");
        }
        else{
            logger.info("Data: Failed to add new sku: {}", sku);
            return ResponseEntity.status(400).body("failure");
        }

    }

    @GetMapping({"/update"})
    public ResponseEntity<String> isUpdating() {
        String state = stateService.getState(IS_UPDATING_KEY);
        logger.info("Current State: {}", state);
        
        // Initialize state if null
        if (state == null) {
            stateService.setState(IS_UPDATING_KEY, "false");
            state = "false";
        }
        
        return ResponseEntity.ok(state);
    }

    @PostMapping("/update/start")
    public ResponseEntity<String> startUpdate() {
        try {
            String currentState = stateService.getState(IS_UPDATING_KEY);
            
            if (currentState == null || currentState.equals("false")) {
                stateService.setState(IS_UPDATING_KEY, "true");
            } else if (currentState.equals("true")) {
                return ResponseEntity.status(409).body("Update already in progress");
            }
            
            logger.info("Starting inventory update process");
        
            actions.updateInventory();
            logger.info("Inventory update completed successfully");
            stateService.setState(IS_UPDATING_KEY, "false");
            return ResponseEntity.ok("Inventory updated successfully");
                
        } catch (Exception e) {
            logger.error("Error occurred during update process", e);
            // Ensure state is reset even if finally block somehow fails
            try {
                stateService.setState(IS_UPDATING_KEY, "false");
            } catch (Exception ex) {
                logger.error("Failed to reset state after error", ex);
            }
            return ResponseEntity.status(500).body("Error updating inventory: " + e.getMessage());
        }
    }

    @PostMapping("/update/stop")
    public ResponseEntity<String> stopUpdate() {
        try {
            stateService.setState(IS_UPDATING_KEY, "false");
            logger.info("Update process stopped");
            return ResponseEntity.ok("Update process stopped successfully");
            
        } catch (Exception e) {
            logger.error("Error occurred while stopping update process", e);
            return ResponseEntity.status(500).body("Error stopping update: " + e.getMessage());
        }
    }

    @PostMapping("/update/{sku}")
    public ResponseEntity<String> updateAndPushItem(@PathVariable String sku) {
        actions.updateAndPushItem(sku);
        return ResponseEntity.ok("Item updated successfully");
    }

    @GetMapping("/square/{mpn}")
    public ResponseEntity<String> getInventoryCountBySku(@PathVariable String mpn){
        try{        
            return ResponseEntity.ok(square.getInventoryCountByMpn(mpn));
        } catch (Exception e){
            return ResponseEntity.status(500).body("status: failed to get mpn");
        }
    }

    @DeleteMapping({"/{sku}"})
    public ResponseEntity<Map<String,String>> deleteItemBySku(@PathVariable String sku){

        if(db.deleteItem(sku)){
            return ResponseEntity.ok(Map.of("message", "success"));
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "failed"));
    }
}