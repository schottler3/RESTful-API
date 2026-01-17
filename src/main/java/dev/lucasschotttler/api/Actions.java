package dev.lucasschotttler.api;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dev.lucasschotttler.database.Databasing;
import dev.lucasschotttler.database.Databasing.DatabaseItem;
import dev.lucasschotttler.update.Lakes;
import dev.lucasschotttler.update.Lakes.LakesItem;

@Service
public class Actions {
    
    private final Databasing db;
    private final Lakes lakes;
    private static final Logger logger = LoggerFactory.getLogger(SkuController.class);

    public Actions(Databasing db, Lakes lakes){
        this.db = db;
        this.lakes = lakes;
    }

    public void updateInventory(){
        List<Map<String, Object>> data = db.queryDatabase("", 10000);

        data.forEach(item -> {
            DatabaseItem dbItem = db.new DatabaseItem();
            
            dbItem.lakesid = (int) item.get("lakesid");
            dbItem.width = (double) item.get("width");
            dbItem.length = (double) item.get("length");
            dbItem.height = (double) item.get("height");
            dbItem.weight = (double) item.get("weight");
            dbItem.type = (String) item.get("type");
            dbItem.mpn = (String) item.get("mpn");
            dbItem.title = (String) item.get("title");
            dbItem.description = (String) item.get("description");
            dbItem.upc = (String) item.get("upc");
            dbItem.quantity = (int) item.get("quantity");
            dbItem.sku = (String) item.get("sku");
            dbItem.updated_at = (Date) item.get("updated_at");
            dbItem.milwaukee_images = (String) item.get("milwaukee_images");
            dbItem.package_width = (double) item.get("package_width");
            dbItem.package_length = (double) item.get("package_length");
            dbItem.package_height = (double) item.get("package_height");
            dbItem.package_weight = (double) item.get("package_weight");
            dbItem.custom_description = (String) item.get("custom_description");
            dbItem.lakes_images = (String) item.get("lakes_images");
            dbItem.minimum_price = (double) item.get("minimum_price");
            dbItem.calculated_price = (double) item.get("calculated_price");
            dbItem.maximum_price = (double) item.get("maximum_price");
            dbItem.lakes_price = (double) item.get("lakes_price");

            final LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);
            
            if(lakesItem.quantity != dbItem.quantity){
                logger.info("Quantity Updated: {} -> {}", dbItem.quantity, lakesItem.quantity);
                dbItem.quantity = lakesItem.quantity;
            }

            if(lakesItem.price != dbItem.calculated_price)
            
        });
    }

}
