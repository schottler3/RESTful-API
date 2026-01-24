package dev.lucasschotttler.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dev.lucasschotttler.database.Databasing;
import dev.lucasschotttler.lakes.Lakes;
import dev.lucasschotttler.lakes.LakesItem;
import dev.lucasschotttler.update.Amazon;
import dev.lucasschotttler.update.Ebay;
import dev.lucasschotttler.database.DatabaseItem;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);

    public Actions(Databasing db, Lakes lakes){
        this.db = db;
    }

    public void updateInventory(){
        logger.info("Actions getting database...");
        List<Map<String, Object>> data = db.queryDatabase("", 10000);
        logger.info("Actions recieved database...");

        if(data.size() <= 0){
            logger.warn("Actions database data is empty!");
        }

        data.forEach(item -> {
            logger.info("Actions starting on item: {}", item);
            DatabaseItem dbItem = new DatabaseItem(item);

            logger.info("Actions resolved dbItem for sku: {}", dbItem.sku);

            LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);

            logger.info("Actions resolved lakesItem for sku: {}", lakesItem.sku);
            
            dbItem.updateItem(lakesItem, db);

            logger.info("Actions resolved updateItem on database for sku: {}", dbItem.sku);

            Amazon.updateItem(dbItem);

            logger.info("Actions resolved updateItem on Amazon for sku: {}", dbItem.sku);

            Ebay.updateItem(dbItem);

            logger.info("Actions resolved updateItem on Ebay for sku: {}", dbItem.sku);

        });
    }

}
