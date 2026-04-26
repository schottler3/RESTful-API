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
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Map<String,String>> addInventoryItem(@RequestBody(required = true) Map<String, String> requestBody) {

        String lakesid = requestBody.get("lakesid");
        String marketplaces = requestBody.get("marketplaces");

        Map<String, String> response = new HashMap<>();

        if(lakesid == null){
            //logger.info("ReportController: Received request to add all new report item to inventory");

            List<Integer> allNewLakesids = db.getAllReportIds("new");

            for (Integer newItem : allNewLakesids) {
                LakesItem item = lakes.getLakesItem(newItem);

                //logger.info("ReportController: Successfully pulled LakesItem from lakesid with sku: {}", item.sku);

                DatabaseItem dbItem = new DatabaseItem(item);

                //logger.info("ReportController: Successfully made and casted DatabaseItem from LakesItem. sku: {}", dbItem.sku);

                if(db.createItem(dbItem, marketplaces)){
                    logger.info("Report item added successfully: {}", item.lakesid);
                }
                else{
                    logger.info("Report items failed to be added: {}", item.lakesid);
                    return ResponseEntity.badRequest().build();
                }
            }
            response.put("status", "success");
            return ResponseEntity.ok(response);
        }
        if(marketplaces == null){
            response.put("status", "marketplaces");
            return ResponseEntity.status(400).body(response);
        }

        //logger.info("ReportController: Received request to add new report item to inventory. Lakesid: {}", lakesid);

        LakesItem item = lakes.getLakesItem(Integer.parseInt(lakesid));

        //logger.info("ReportController: Successfully pulled LakesItem from lakesid with sku: {}", item.sku);

        DatabaseItem dbItem = new DatabaseItem(item);

        //logger.info("ReportController: Successfully made and casted DatabaseItem from LakesItem. sku: {}", dbItem.sku);

        if(db.createItem(dbItem, marketplaces) && db.deleteReportItem(Integer.parseInt(lakesid))){
            logger.info("Report item added successfully and deleted from report: {}", lakesid);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        }
        else{
            logger.info("Report item failed to be added: {}", lakesid);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check/{sku}")
    public ResponseEntity<List<Map<String,Object>>> checkForExistingReportSkus(@PathVariable String sku){

        //logger.info("Report: Getting potentially existing skus for a given sku: {}", sku);

        List<Map<String,Object>> potentialMatches = db.checkReportForExistingSku(sku);

        if (potentialMatches == null || potentialMatches.isEmpty()) {
            logger.info("Report: Generated list for potentially existing skus for a given sku: {}, but it was empty!", sku);
            return ResponseEntity.noContent().build();
        } else {
            logger.info("Report: Generated list for potentially existing skus for a given sku: {}", sku);
            return ResponseEntity.ok(potentialMatches);
        }
    }
}
