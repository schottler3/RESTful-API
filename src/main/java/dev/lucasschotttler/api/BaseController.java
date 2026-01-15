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
import dev.lucasschotttler.database.postgreSQL;

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

    private final postgreSQL db;

    public SuperiorController(postgreSQL db) {
        this.db = db;
    }

    @GetMapping("/update")
    public ResponseEntity<String> update() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(db.createEntries());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during update: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update interrupted: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }
}

@RestController
@RequestMapping("/superior/data")
class SkuController {

    private final postgreSQL db;
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);


    public SkuController(postgreSQL db) {
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
    public ResponseEntity<?> patchRoot(@RequestBody(required = true) Map<String, Object> requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Request body is missing or empty\"}");
        }

        logger.info("Received PATCH request with body: " + requestBody);

        try {
            
            return ResponseEntity.ok("{\"message\":\"Patch request processed successfully\"}");
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

    private final postgreSQL db;

    public ImageController(postgreSQL db) {
        this.db = db;
    }

    @GetMapping("/{SKU}")
    public ResponseEntity<List<String>> tools(@PathVariable String SKU) {
        return ResponseEntity.status(HttpStatus.OK).body(db.getImages(SKU));
    }
}