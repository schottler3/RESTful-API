package dev.lucasschotttler.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschotttler.database.DatabaseItem;
import dev.lucasschotttler.database.Databasing;
import dev.lucasschotttler.lakes.Lakes;
import dev.lucasschotttler.lakes.LakesItem;
import dev.lucasschotttler.update.Amazon;
import dev.lucasschotttler.update.Ebay;

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

    private final Databasing db;
    private final Actions actions;
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);

    public SuperiorController(Databasing db, Actions actions) {
        this.db = db;
        this.actions = actions;
    }

    @GetMapping("/update")
    public ResponseEntity<String> update() {
        try {
            actions.updateInventory();
            return ResponseEntity.ok("Inventory updated successfully");
        } catch (Exception e){
            logger.error("This shouldn't be possible atm");
        }
        return null;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }
}

@RestController
@RequestMapping("/superior/lakes")
class LakesController {

    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);
    private final Databasing db;

    public LakesController(Databasing db) {
        this.db = db;
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<?> lakesEnd(
        @RequestParam(required = true) int id
    ) {
        logger.info("Received request - {}: ", id);

        List<Map<String, Object>> items = db.queryDatabase(String.valueOf(id), 1);
        Map<String, Object> item = items.get(0);

        DatabaseItem dbItem = new DatabaseItem(item);

        System.out.println(dbItem);

        LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);
        
        dbItem.updateItem(lakesItem, db);

        Amazon.updateItem(dbItem);

        //Ebay.updateItem(dbItem);
    
        return null;
    }
}

@RestController
@RequestMapping("/superior/data")
class SkuController {

    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);

    public SkuController(Databasing db) {
        this.db = db;
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<?> dataRoot(
        @RequestParam(required = false) String SKU,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String keywords
    ) {
        logger.info("Received request - SKU: " + SKU + ", Limit: " + limit + ", Keywords: " + keywords);

        int resultLimit = (limit != null) ? limit : 100;
        List<java.util.Map<String, Object>> results;

        try {
            if (SKU != null) {
                logger.info("Fetching data by SKU...");
                results = db.getData(SKU, resultLimit);
            } 
            else {
                logger.info("Fetching data by keywords...");
                results = db.queryDatabase(keywords, resultLimit);
            }

            if (results != null && results.size() > 0) {
                logger.info("Data fetched successfully. Returning results.");
                return ResponseEntity.ok(results);
            } else {
                logger.warn("No data found for the given parameters.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("404: Not Found");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: " + e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Internal server error\"}");
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