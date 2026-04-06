package dev.lucasschottler.api.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;

@RestController
@RequestMapping("/superior/alternative")
public class AltController {

    private Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(AltController.class);

    public AltController(Databasing db){
        this.db = db;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> getAlts(@RequestBody(required = true) Map<String, String> requestBody) {

        String mpn = requestBody.get("mpn");
        String sku = requestBody.get("sku");

        if (sku == null) {
            return ResponseEntity.status(400).body("{\"message\": \"sku invalid or missing\"}");
        }
        else if (mpn == null) {
            return ResponseEntity.status(400).body("{\"message\": \"mpn invalid or missing\"}");
        }

        logger.info("Alternative received request for getting all mpn: {}, sku: {}", mpn, sku);

        return ResponseEntity.ok(db.getAlts(mpn, sku));
    }
    
    @PostMapping("/add")
    public ResponseEntity<String> addAlt(@RequestBody(required = true) Map<String, String> requestBody){

        String mpn = requestBody.get("mpn");
        String alt_sku = requestBody.get("alt_sku");
        String marketplaces = requestBody.get("marketplaces");

        String response = String.format("{\"message\": \"request failed\"}");

        if(alt_sku == null){
            response = String.format("{\"message\": \"alt_sku invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(mpn == null){
            response = String.format("{\"message\": \"mpn invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(marketplaces == null){
            response = String.format("{\"message\": \"marketplaces invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }

        logger.info("Alternative received request for adding a new alternative. mpn: {}, altSku: {}", mpn, alt_sku);

        DatabaseItem dbItem = new DatabaseItem(db.getData(mpn));
        dbItem.sku = alt_sku;

        if(db.createItem(dbItem, marketplaces)){
            response = String.format("{\"message\": \"%s created\"}", alt_sku);

            logger.info("Alternative successfully created with altSku: {}", alt_sku);

            return ResponseEntity.ok(response);
        }
        else {
            response = String.format("{\"message\": \"%s failed to create\"}", alt_sku);

            logger.info("Alternative failed to create with altSku: {}", alt_sku);

            return ResponseEntity.status(400).body(response);
        }

        

    }

}
