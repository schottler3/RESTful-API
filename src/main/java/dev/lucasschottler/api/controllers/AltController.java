package dev.lucasschottler.api.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{parentSku}")
    public ResponseEntity<List<java.util.Map<String,Object>>> getAlts(@PathVariable String parentSku){

        logger.info("Alternative received request for getting all: {}", parentSku);
        
        return ResponseEntity.ok(db.getAlts(parentSku));
    }
    
    @PostMapping("/add")
    public ResponseEntity<String> addAlt(@RequestBody(required = true) Map<String, String> requestBody){

        String parent_sku = requestBody.get("parent_sku");
        String alt_sku = requestBody.get("alt_sku");

        String response = String.format("{\"message\": \"request failed\"}");

        if(alt_sku == null){
            response = String.format("{\"message\": \"alt_sku invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(parent_sku == null){
            response = String.format("{\"message\": \"sku invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }

        logger.info("Alternative received request for adding a new alternative. parentSku: {}, altSku: {}", parent_sku, alt_sku);

        DatabaseItem dbItem = new DatabaseItem(db.getData(parent_sku));
        dbItem.sku = alt_sku;

        if(db.createAlt(dbItem,parent_sku)){
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
