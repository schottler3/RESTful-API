package dev.lucasschottler.api.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.api.report.Report;
import dev.lucasschottler.database.DatabaseItem;
import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;

@RestController
@RequestMapping("/superior/report")
public class ReportController {

    private Databasing db;
    private Lakes lakes;
    private final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private Report report;

    public ReportController(Databasing db, Lakes lakes, Report report){
        this.db = db;
        this.lakes = lakes;
        this.report = report;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<Map<String,Object>>> getReport(){
        return ResponseEntity.ok(db.getReport());
    }
    
    @GetMapping("/new")
    public ResponseEntity<List<LakesItem>> getNewReport() {

        List<LakesItem> newItems = report.getNewItems();

        if (newItems == null || newItems.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(newItems);
        }
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String,String>> addAllToInventory(){

        logger.info("ReportController: Received request to add all new report item to inventory");

        List<Integer> allNewLakesids = db.getAllReportIds("new");

        for (Integer newItem : allNewLakesids) {
            LakesItem item = lakes.getLakesItem(newItem);

            logger.info("ReportController: Successfully pulled LakesItem from lakesid with sku: {}", item.sku);

            DatabaseItem dbItem = new DatabaseItem(item);

            logger.info("ReportController: Successfully made and casted DatabaseItem from LakesItem. sku: {}", dbItem.sku);

            if(db.createItem(dbItem)){
                logger.info("Report item added successfully: {}", item.lakesid);
            }
            else{
                logger.info("Report items failed to be added: {}", item.lakesid);
                return ResponseEntity.badRequest().build();
            }
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/new/{lakesid}")
    public ResponseEntity<Map<String,String>> addInventoryItem(@PathVariable int lakesid){

        logger.info("ReportController: Received request to add new report item to inventory. Lakesid: {}", lakesid);

        LakesItem item = lakes.getLakesItem(lakesid);

        logger.info("ReportController: Successfully pulled LakesItem from lakesid with sku: {}", item.sku);

        DatabaseItem dbItem = new DatabaseItem(item);

        logger.info("ReportController: Successfully made and casted DatabaseItem from LakesItem. sku: {}", dbItem.sku);

        if(db.createItem(dbItem) && db.deleteReportItem(lakesid)){
            logger.info("Report item added successfully and deleted from report: {}", lakesid);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        }
        else{
            logger.info("Report item failed to be added: {}", lakesid);
            return ResponseEntity.badRequest().build();
        }
    }

}
