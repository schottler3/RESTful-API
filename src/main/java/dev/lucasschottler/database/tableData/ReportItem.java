package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.lucasschottler.database.Databasing;
import dev.lucasschottler.database.queries.ReportQueries;
import dev.lucasschottler.lakes.Lakes;
import dev.lucasschottler.lakes.LakesItem;
import lombok.Data;
import lombok.NoArgsConstructor;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Data
@NoArgsConstructor
public class ReportItem {

    private static final Logger logger = LoggerFactory.getLogger(ReportItem.class);
    private static final Lakes lakes = new Lakes();
    
    public Integer lakesid;
    public String title;
    public String description;
    public Integer quantity;
    public String sku;
    public String lakes_images;
    public Double lakes_price;
    public Timestamp date_added;
    public String type;

    public ReportItem(Map<String, Object> item, String type){
        if(item != null){
            //logger.info("ReportItem: Parsing report item: {}", item.toString());

            this.lakesid = (Integer) item.get("lakesid");
            this.title = (String) item.get("title");
            this.description = (String) item.get("description");
            this.quantity = (Integer) item.get("quantity");
            this.sku = (String) item.get("sku");
            this.date_added = (Timestamp) item.get("date_added");
            this.lakes_images = (String) item.get("lakes_images");
            this.lakes_price = (Double) item.get("lakes_price");
            this.type = type;
        }
    }

    public static List<LakesItem> getNewItems(Databasing db, ReportQueries reportQueries) {

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
                        reportQueries.addReportNewItem(lakesItem);
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
                reportQueries.addReportNewItem(lakesItem);
                newItems.add(lakesItem);
            } else {
                concurrentFails++;
            }
        }

        logger.info("Total new items found: {}", newItems.size());
        return newItems;
    }

}
