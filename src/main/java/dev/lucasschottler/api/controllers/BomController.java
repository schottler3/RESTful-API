package dev.lucasschottler.api.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.Databasing;

@RestController
@RequestMapping("/superior/bom")
class BomController {

    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(BomController.class);

    public BomController(Databasing db) {
        this.db = db;
    }

    @GetMapping("/{parent_sku}")
    public ResponseEntity<List<Map<String, Object>>> getBomData(@PathVariable String parent_sku) {
        return ResponseEntity.status(HttpStatus.OK).body(db.getBom(parent_sku));
    }

    @PutMapping(value = "/add/{parent_sku}", consumes = "application/json")
    public ResponseEntity<?> updateBomDependencies(@PathVariable String parent_sku, @RequestBody(required = true) List<Map<String, Object>> requestBody) {

        if (requestBody == null || requestBody.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body is missing or empty"));
        }

        logger.info("Received PUT request with {} bom items", requestBody.size());

        List<Map<String, Object>> failures = new java.util.ArrayList<>();

        try {
            for (int i = 0; i < requestBody.size(); i++) {
                Map<String, Object> bomItem = requestBody.get(i);
                String child_sku = (String) bomItem.get("child_sku");
                Object quantityObj = bomItem.get("quantity");
                
                Double quantity = null;
                if (quantityObj instanceof Integer) {
                    quantity = ((Integer) quantityObj).doubleValue();
                } else if (quantityObj instanceof Double) {
                    quantity = (Double) quantityObj;
                } else if (quantityObj instanceof Number) {
                    quantity = ((Number) quantityObj).doubleValue();
                }

                if (child_sku == null || quantity == null) {
                    logger.warn("Missing required fields in PUT: " + i);
                    failures.add(bomItem);
                    continue;
                }

                if(quantity == -1){
                    int removed = db.removeBom(parent_sku, child_sku);
                    if (removed > 0) {
                        logger.info("Success on bom removal - parent_id: {}, child_id: {}", parent_sku, child_sku);
                    } else if (removed == 0) {
                        logger.info("BOM item not found (skipping) - parent_id: {}, child_id: {}", parent_sku, child_sku);
                    } else {
                        logger.error("Failure on bom removal - parent_id: {}, child_id: {}", parent_sku, child_sku);
                        failures.add(bomItem);
                    }
                    continue;
                }
                
                logger.info("Attempt on bom addition - parent_id: {}, child_id: {}, quantity: {}", parent_sku, child_sku, quantity);
                if(db.addBom(parent_sku, child_sku, quantity)){
                    logger.info("Success on bom addition - parent_id: {}, child_id: {}, quantity: {}", parent_sku, child_sku, quantity);
                } else {
                    logger.info("Failure on bom addition - parent_id: {}, child_id: {}, quantity: {}", parent_sku, child_sku, quantity);
                    failures.add(bomItem);
                }
            }
            
            if(failures.isEmpty()){
                return ResponseEntity.ok(Map.of("message", "All PUTs processed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(Map.of("failures", failures));
            }
        } catch (Exception e) {
            logger.error("Error processing Put request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }
}