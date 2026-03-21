package dev.lucasschottler.api.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;

@RestController
@RequestMapping("/superior/report")
public class ReportController {

    private Databasing db;
    private Lakes lakes;
    private final Logger logger = LoggerFactory.getLogger(ReportController.class);

    public ReportController(Databasing db, Lakes lakes){
        this.db = db;
        this.lakes = lakes;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<Map<String,Object>>> getReport(){
        return ResponseEntity.ok(db.getReport());
    }
    
    @GetMapping("/new")
    public ResponseEntity<List<LakesItem>> getNewReport() {

        List<LakesItem> report = lakes.getNewItems();

        if (report == null || report.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(report);
        }
    }

}
