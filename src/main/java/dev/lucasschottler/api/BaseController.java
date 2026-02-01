package dev.lucasschottler.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.Databasing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class BaseController {

    @GetMapping("/")
    public ResponseEntity<String> endpoints() {
        String endpointInfo = """
            {"endpoints": 
                [
                    "superior"
                ]
            }
        """;
        return ResponseEntity.status(HttpStatus.OK).body(endpointInfo);
    }
}

@RestController
@RequestMapping("/superior")
class SuperiorController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }
}

@RestController
@RequestMapping("/superior/data")
class SkuController {

    private final Databasing db;
    private final Actions actions;
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);

    public SkuController(Databasing db, Actions actions) {
        this.db = db;
        this.actions = actions;
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
                Integer lakesid = (Integer) change.get("lakesid");
                String attribute = (String) change.get("attribute");
                String newValue = (String) change.get("new");

                if (lakesid == null || attribute == null || newValue == null) {
                    logger.warn("Missing required fields in patch: " + i);
                    failures.add(change);
                    continue;
                }
                
                logger.info("Attempt on Change - lakesid: {}, attribute: {}, new: {}", lakesid, attribute, newValue);
                if(db.patchItem(lakesid, attribute, newValue)){
                    logger.info("Success on Change: lakesid: {}, attribute: {}, new: {}", lakesid, attribute, newValue);
                    actions.updateItem(lakesid);
                }
                else{
                    logger.error("Failure on Change: lakesid: {}, attribute: {}, new: {}", lakesid, attribute, newValue);
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

}

@RestController
@RequestMapping("/superior/data/reset")
class ResetController {

    private final Actions actions;
    private final Databasing db;    
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);

    public ResetController(Actions actions, Databasing db) {
        this.actions = actions;
        this.db = db;
    }

    @PutMapping("/{lakesid}")
    public ResponseEntity<?> resetItem(@PathVariable int lakesid) {
        try {
            boolean success = actions.resetItem(lakesid);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Failed to reset item - item not found or reset failed"));
            }
            
            Map<String, Object> updatedItem = db.getData(lakesid, 1);
            
            if (updatedItem == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Item reset but failed to retrieve updated data"));
            }
            
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Error resetting item {}: {}", lakesid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }
}

@RestController
@RequestMapping("/superior/data/update")
class UpdateController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);
    private static final String IS_UPDATING_KEY = "isUpdating";
    
    private final StateService stateService;
    private final Actions actions;

    public UpdateController(StateService stateService, Actions actions) {
        this.stateService = stateService;
        this.actions = actions;
    }

    @GetMapping({"", "/"})
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

    @PostMapping("/start")
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

    @PostMapping("/stop")
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
}

@RestController
@RequestMapping("/superior/images")
class ImageController {

    private final Databasing db;

    public ImageController(Databasing db) {
        this.db = db;
    }

    @GetMapping("/{SKU}")
    public ResponseEntity<List<String>> tools(@PathVariable String SKU) {
        return ResponseEntity.status(HttpStatus.OK).body(db.getImages(SKU));
    }
}