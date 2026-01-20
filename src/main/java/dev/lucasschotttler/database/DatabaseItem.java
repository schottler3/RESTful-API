package dev.lucasschotttler.database;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.lucasschotttler.api.BaseController;
import dev.lucasschotttler.lakes.LakesItem;
import dev.lucasschotttler.update.Amazon;

public class DatabaseItem {

    private final Integer DEFAULT_FULFILLMENT = 5;

    public Integer lakesid;
    public Double width;
    public Double length;
    public Double height;
    public Double weight;
    public String type;
    public String mpn;
    public String title;
    public String description;
    public String upc;
    public Integer quantity;
    public Integer custom_quantity;
    public String sku;
    public Timestamp updated_at;
    public String milwaukee_images;
    public Double package_width;
    public Double package_length;
    public Double package_height;
    public Double package_weight;
    public String custom_description;
    public String lakes_images;
    public Double minimum_price;
    public Double calculated_price;
    public Double maximum_price;
    public Double lakes_price;
    public Double custom_price;
    public Integer fulfillment;

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    //Extraction of data from database Into an object
    public DatabaseItem(Map<String, Object> item){

        this.lakesid = (Integer) item.get("lakesid");
        this.width = (Double) item.get("width");
        this.length = (Double) item.get("length");
        this.height = (Double) item.get("height");
        this.weight = (Double) item.get("weight");
        this.type = (String) item.get("type");
        this.mpn = (String) item.get("mpn");
        this.title = (String) item.get("title");
        this.description = (String) item.get("description");
        this.upc = (String) item.get("upc");
        this.quantity = (Integer) item.get("quantity");
        this.custom_quantity = (Integer) item.get("custom_quantity");
        this.sku = (String) item.get("sku");
        this.updated_at = (Timestamp) item.get("updated_at");
        this.milwaukee_images = (String) item.get("milwaukee_images");
        this.package_width = (Double) item.get("package_width");
        this.package_length = (Double) item.get("package_length");
        this.package_height = (Double) item.get("package_height");
        this.package_weight = (Double) item.get("package_weight");
        this.custom_description = (String) item.get("custom_description");
        this.lakes_images = (String) item.get("lakes_images");
        this.minimum_price = (Double) item.get("minimum_price");
        this.calculated_price = (Double) item.get("calculated_price");
        this.maximum_price = (Double) item.get("maximum_price");
        this.lakes_price = (Double) item.get("lakes_price");
        this.custom_price = (Double) item.get("custom_price");
        this.fulfillment = (Integer) item.get("fulfillment");
    }

    public void updateItem(LakesItem lakesItem){
       
        if (this.quantity == null || this.quantity != lakesItem.quantity) {
            logger.info("Quantity Updated: {} -> {}", this.quantity, lakesItem.quantity);
            this.quantity = lakesItem.quantity;
        }

        if (this.lakes_price == null || this.lakes_price != lakesItem.price || this.lakes_price != this.custom_price) {
            logger.info("Price Updated: {} -> {}", this.lakes_price, lakesItem.price);
            this.lakes_price = lakesItem.price;

            //minimum_price, middle_price, maximum_price
            HashMap<String,Double> amazonPrices = Amazon.getPrices(lakesItem.price);

            this.minimum_price = amazonPrices.get("minimum_price");
            this.calculated_price = amazonPrices.get("middle_price");
            this.maximum_price = amazonPrices.get("maximum_price");
        }

        if (this.width == null || this.width != lakesItem.width) {
            logger.info("Width Updated: {} -> {}", this.width, lakesItem.width);
            this.width = lakesItem.width;
        }

        if (this.length == null || this.length != lakesItem.length) {
            logger.info("Length Updated: {} -> {}", this.length, lakesItem.length);
            this.length = lakesItem.length;
        }

        if (this.height == null || this.height != lakesItem.height) {
            logger.info("Height Updated: {} -> {}", this.height, lakesItem.height);
            this.height = lakesItem.height;
        }

        if (this.weight == null || this.weight != lakesItem.weight) {
            logger.info("Weight Updated: {} -> {}", this.weight, lakesItem.weight);
            this.weight = lakesItem.weight;
        }

        if (this.type == null) {
            logger.info("Type Updated: {} -> {}", this.type, lakesItem.type);
            this.type = lakesItem.type;
        }

        if (this.mpn == null) {
            logger.info("MPN Updated: {} -> {}", this.mpn, lakesItem.mpn);
            this.mpn = lakesItem.mpn;
        }

        if (this.title == null) {
            logger.info("Title Updated: {} -> {}", this.title, lakesItem.title);
            this.title = lakesItem.title;
        }

        if (this.description == null) {
            logger.info("Description Updated: {} -> {}", this.description, lakesItem.description);
            this.description = lakesItem.description;
        }

        if (this.upc == null) {
            logger.info("UPC Updated: {} -> {}", this.upc, lakesItem.upc);
            this.upc = lakesItem.upc;
        }

        if (this.sku == null) {
            logger.info("SKU Updated: {} -> {}", this.sku, lakesItem.sku);
            this.sku = lakesItem.sku;
        }

        if (this.lakes_images == null){
            logger.info("Lakes Image Updated: {} -> {}", this.lakes_images, lakesItem.imageLink);
            this.lakes_images = lakesItem.imageLink;
        }

        if (this.fulfillment == null){
            this.fulfillment = DEFAULT_FULFILLMENT;
        }
    }

    @Override
    public String toString() {
        return "DatabaseItem{" +
                "lakesid=" + lakesid +
                ", width=" + width +
                ", length=" + length +
                ", height=" + height +
                ", weight=" + weight +
                ", type='" + type + '\'' +
                ", mpn='" + mpn + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", upc='" + upc + '\'' +
                ", quantity=" + quantity +
                ", custom_quantity=" + custom_quantity +
                ", sku='" + sku + '\'' +
                ", updated_at=" + updated_at +
                ", milwaukee_images='" + milwaukee_images + '\'' +
                ", package_width=" + package_width +
                ", package_length=" + package_length +
                ", package_height=" + package_height +
                ", package_weight=" + package_weight +
                ", custom_description='" + custom_description + '\'' +
                ", lakes_images='" + lakes_images + '\'' +
                ", minimum_price=" + minimum_price +
                ", calculated_price=" + calculated_price +
                ", maximum_price=" + maximum_price +
                ", lakes_price=" + lakes_price +
                ", custom_price=" + custom_price +
                ", fulfillment=" + fulfillment +
                '}';
    }

}
