package dev.lucasschotttler.api;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        logger.info("Sku: " + SKU + " Limit: " + limit);

        if (SKU != null && limit != null) {
            return ResponseEntity.status(HttpStatus.OK).body(db.getData(SKU, limit));
        } 
        else if (SKU != null) {
            return ResponseEntity.status(HttpStatus.OK).body(db.getData(SKU));
        } 
        else if (limit != null) {
            return ResponseEntity.status(HttpStatus.OK).body(db.getData(limit));
        }
        else if (keywords != null){
            return ResponseEntity.status(HttpStatus.OK).body(db.queryDatabase(keywords));
        }
        else {
            return ResponseEntity.ok("Data root endpoint. Use /sku/limit with SKU and/or limit query parameters.");
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