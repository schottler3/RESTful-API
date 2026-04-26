package dev.lucasschottler.api.report;

import java.sql.Timestamp;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportItem {

    private final Logger logger = LoggerFactory.getLogger(ReportItem.class);
    
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

}
