package dev.lucasschottler.api.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.DatabaseItem;

import dev.lucasschottler.api.alternative.Alternative;
import dev.lucasschottler.database.Databasing;

@RestController
@RequestMapping("/superior/alternative")
public class AltController {

    private Databasing db;

    public AltController(Databasing db){
        this.db = db;
    }
    
    @PostMapping("/add")
    public ResponseEntity<String> addAlt(@RequestBody(required = true) Map<String, String> requestBody){

        String lakesidStr = requestBody.get("lakesid");
        String alt_sku = requestBody.get("alt_sku");
        String is_ebay_str = requestBody.get("is_ebay");
        String is_amazon_str = requestBody.get("is_amazon");

        String response = String.format("{\"message\": \"request failed\"}");

        if(alt_sku == null){
            response = String.format("{\"message\": \"alt_sku invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(lakesidStr == null){
            response = String.format("{\"message\": \"lakesid invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(is_ebay_str == null){
            response = String.format("{\"message\": \"is_ebay invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }
        else if(is_amazon_str == null){
            response = String.format("{\"message\": \"is_amazon invalid or missing\"}");
            return ResponseEntity.status(400).body(response);
        }

        Integer lakesid = Integer.parseInt(lakesidStr);
        Boolean is_ebay = Boolean.parseBoolean(is_ebay_str);
        Boolean is_amazon = Boolean.parseBoolean(is_amazon_str);

        DatabaseItem dbItem = new DatabaseItem(db.getData(lakesid, 1));
        Alternative altItem = new Alternative(dbItem, alt_sku, is_ebay, is_amazon);

        db.createAlt(altItem);

        response = String.format("{\"message\": \"%s created\"}", alt_sku);

        return ResponseEntity.ok(response);

    }

}
