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
        int lastId = db.getLastLakesId();

        if(lastId <= 0){
            logger.info("Lakes: the smallest new index was not found...");
            return null;
        }

        //logger.info("Last lakesid from DB: {}", lastId);

        List<LakesItem> newItems = new ArrayList<>();
        int delta = 1;
        int concurrentFails = 0;

        while (concurrentFails < 10) {
            int queryId = lastId + delta;
            //logger.info("Querying lakesid: {}", queryId);

            ReportItem rItem = new ReportItem(db.getReport(queryId, "new"), null);

            if(rItem.lakesid != null){
                delta++;
                continue;
            }
            
            LakesItem newItem = lakes.getLakesItem(queryId);

            if (newItem != null) {
                //logger.info("Found item with lakesid: {}", newItem.lakesid);
                newItems.add(newItem);
                db.addReportNewItem(newItem);
                concurrentFails = 0;
            } else {
                logger.info("No item found for lakesid: {}, stopping loop", queryId);
                concurrentFails++;
            }
            delta++;
        }

        logger.info("Total new items found: {}", newItems.size());
        return newItems;
    }
}
