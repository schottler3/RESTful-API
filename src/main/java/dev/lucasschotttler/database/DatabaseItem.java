package dev.lucasschotttler.database;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.lucasschotttler.api.BaseController;
import dev.lucasschotttler.update.Amazon;
import dev.lucasschotttler.update.Lakes.LakesItem;

public class DatabaseItem {

    public int lakesid;
    public double width;
    public double length;
    public double height;
    public double weight;
    public String type;
    public String mpn;
    public String title;
    public String description;
    public String upc;
    public int quantity;
    public int custom_quantity;
    public String sku;
    public Date updated_at;
    public String milwaukee_images;
    public double package_width;
    public double package_length;
    public double package_height;
    public double package_weight;
    public String custom_description;
    public String lakes_images;
    public double minimum_price;
    public double calculated_price;
    public double maximum_price;
    public double lakes_price;
    public double custom_price;

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    //Extraction of data from database into an object
    public DatabaseItem(Map<String, Object> item){

        this.lakesid = (int) item.get("lakesid");
        this.width = (double) item.get("width");
        this.length = (double) item.get("length");
        this.height = (double) item.get("height");
        this.weight = (double) item.get("weight");
        this.type = (String) item.get("type");
        this.mpn = (String) item.get("mpn");
        this.title = (String) item.get("title");
        this.description = (String) item.get("description");
        this.upc = (String) item.get("upc");
        this.quantity = (int) item.get("quantity");
        this.custom_quantity = (int) item.get("custom_quantity");
        this.sku = (String) item.get("sku");
        this.updated_at = (Date) item.get("updated_at");
        this.milwaukee_images = (String) item.get("milwaukee_images");
        this.package_width = (double) item.get("package_width");
        this.package_length = (double) item.get("package_length");
        this.package_height = (double) item.get("package_height");
        this.package_weight = (double) item.get("package_weight");
        this.custom_description = (String) item.get("custom_description");
        this.lakes_images = (String) item.get("lakes_images");
        this.minimum_price = (double) item.get("minimum_price");
        this.calculated_price = (double) item.get("calculated_price");
        this.maximum_price = (double) item.get("maximum_price");
        this.lakes_price = (double) item.get("lakes_price");
        this.custom_price = (double) item.get("custom_price");

    }

    public void updateItem(DatabaseItem dbItem, LakesItem lakesItem){
       
        if (dbItem.quantity != lakesItem.quantity) {
            logger.info("Quantity Updated: {} -> {}", dbItem.quantity, lakesItem.quantity);
            dbItem.quantity = lakesItem.quantity;
        }

        if (dbItem.lakes_price != lakesItem.price || dbItem.lakes_price != dbItem.custom_price) {
            logger.info("Price Updated: {} -> {}", dbItem.lakes_price, lakesItem.price);
            dbItem.lakes_price = lakesItem.price;

            //minimum_price, middle_price, maximum_price
            HashMap<String,Double> amazonPrices = Amazon.getPrices(lakesItem.price);

            dbItem.minimum_price = amazonPrices.get("minimum_price");
            dbItem.calculated_price = amazonPrices.get("middle_price");
            dbItem.maximum_price = amazonPrices.get("maximum_price");
        }

        if (dbItem.width != lakesItem.width) {
            logger.info("Width Updated: {} -> {}", dbItem.width, lakesItem.width);
            dbItem.width = lakesItem.width;
        }

        if (dbItem.length != lakesItem.length) {
            logger.info("Length Updated: {} -> {}", dbItem.length, lakesItem.length);
            dbItem.length = lakesItem.length;
        }

        if (dbItem.height != lakesItem.height) {
            logger.info("Height Updated: {} -> {}", dbItem.height, lakesItem.height);
            dbItem.height = lakesItem.height;
        }

        if (dbItem.weight != lakesItem.weight) {
            logger.info("Weight Updated: {} -> {}", dbItem.weight, lakesItem.weight);
            dbItem.weight = lakesItem.weight;
        }

        if (!dbItem.type.equals(lakesItem.type)) {
            logger.info("Type Updated: {} -> {}", dbItem.type, lakesItem.type);
            dbItem.type = lakesItem.type;
        }

        if (!dbItem.mpn.equals(lakesItem.mpn)) {
            logger.info("MPN Updated: {} -> {}", dbItem.mpn, lakesItem.mpn);
            dbItem.mpn = lakesItem.mpn;
        }

        if (!dbItem.title.equals(lakesItem.title)) {
            logger.info("Title Updated: {} -> {}", dbItem.title, lakesItem.title);
            dbItem.title = lakesItem.title;
        }

        if (!dbItem.description.equals(lakesItem.description)) {
            logger.info("Description Updated: {} -> {}", dbItem.description, lakesItem.description);
            dbItem.description = lakesItem.description;
        }

        if (!dbItem.upc.equals(lakesItem.upc)) {
            logger.info("UPC Updated: {} -> {}", dbItem.upc, lakesItem.upc);
            dbItem.upc = lakesItem.upc;
        }

        if (!dbItem.sku.equals(lakesItem.sku)) {
            logger.info("SKU Updated: {} -> {}", dbItem.sku, lakesItem.sku);
            dbItem.sku = lakesItem.sku;
        }

        if (!dbItem.lakes_images.equals(lakesItem.imageLink)){
            logger.info("Lakes Image Updated: {} -> {}", dbItem.lakes_images, lakesItem.imageLink);
            dbItem.lakes_images = lakesItem.imageLink;
        }
    }

}
