package dev.lucasschottler.api.report;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;

@Service
public class Report {

    private static final Logger logger = LoggerFactory.getLogger(Report.class);
    private Databasing db;
    private Lakes lakes;

    public Report(Databasing db, Lakes lakes){
        this.db = db;
        this.lakes = lakes;
    }

    public List<LakesItem> getNewItems() {

        List<LakesItem> newItems = new ArrayList<>();
        
        Integer[] lakesIds = db.getAllLakesIdsInAsc();

        for(int i = 1; i < lakesIds.length; i++){

            int diff = lakesIds[i] - lakesIds[i-1];

            logger.info("Report: Current lakesid: {}", lakesIds[i-1]);

            if(diff > 0){
                for(int j = 1; j < diff; j++){
                    int lakesIdToAdd = lakesIds[i-1] + j;
                    logger.info("Report: Requesting lakesid: {}", lakesIdToAdd);
                    LakesItem lakesItem = lakes.getLakesItem(lakesIdToAdd);
                    if(lakesItem != null){
                        db.addReportNewItem(lakesItem);
                        newItems.add(lakesItem);
                    }
                }
            }

        }

        int concurrentFails = 0;
        int currentId = lakesIds[lakesIds.length];

        while(concurrentFails < 20){
            LakesItem lakesItem = lakes.getLakesItem(currentId++);
            if(lakesItem != null){
                db.addReportNewItem(lakesItem);
                newItems.add(lakesItem);
            } else {
                concurrentFails++;
            }
        }

        logger.info("Total new items found: {}", newItems.size());
        return newItems;
    }
}
