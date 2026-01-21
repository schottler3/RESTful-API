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

    public void updateItem(LakesItem lakesItem, Databasing db){
       
        if (this.quantity == null || this.quantity != lakesItem.quantity) {
            logger.info("Quantity Updated: {} -> {}", this.quantity, lakesItem.quantity);
            this.quantity = lakesItem.quantity;

            if (!db.patchItem(this.lakesid, "quantity", String.valueOf(this.quantity))){
                logger.warn("Database Item UPDATE failure on attribute = quantity: lakesid = {}", this.lakesid);
            }
        }

        if (this.lakes_price == null || this.lakes_price != lakesItem.price || this.lakes_price != this.custom_price) {
            logger.info("Price Updated: {} -> {}", this.lakes_price, lakesItem.price);
            this.lakes_price = lakesItem.price;

            //minimum_price, middle_price, maximum_price
            HashMap<String,Double> amazonPrices = Amazon.getPrices(lakesItem.price);

            this.minimum_price = amazonPrices.get("minimum_price");
            this.calculated_price = amazonPrices.get("middle_price");
            this.maximum_price = amazonPrices.get("maximum_price");

            if (!db.patchItem(this.lakesid, "lakes_price", String.valueOf(this.lakes_price))){
                logger.warn("Database Item UPDATE failure on attribute = lakes_price: lakesid = {}", this.lakesid);
            }
            if (!db.patchItem(this.lakesid, "minimum_price", String.valueOf(this.minimum_price))){
                logger.warn("Database Item UPDATE failure on attribute = minimum_price: lakesid = {}", this.lakesid);
            }
            if (!db.patchItem(this.lakesid, "calculated_price", String.valueOf(this.calculated_price))){
                logger.warn("Database Item UPDATE failure on attribute = calculated_price: lakesid = {}", this.lakesid);
            }
            if (!db.patchItem(this.lakesid, "maximum_price", String.valueOf(this.maximum_price))){
                logger.warn("Database Item UPDATE failure on attribute maximum_price: lakesid = {}", this.lakesid);
            }
        }

        if (this.width == null || this.width != lakesItem.width) {
            logger.info("Width Updated: {} -> {}", this.width, lakesItem.width);
            this.width = lakesItem.width;

            if (!db.patchItem(this.lakesid, "width", String.valueOf(this.width))){
                logger.warn("Database Item UPDATE failure on attribute = width: lakesid = {}", this.lakesid);
            }
        }

        if (this.length == null || this.length != lakesItem.length) {
            logger.info("Length Updated: {} -> {}", this.length, lakesItem.length);
            this.length = lakesItem.length;

            if (!db.patchItem(this.lakesid, "length", String.valueOf(this.length))){
                logger.warn("Database Item UPDATE failure on attribute = length: lakesid = {}", this.lakesid);
            }
        }

        if (this.height == null || this.height != lakesItem.height) {
            logger.info("Height Updated: {} -> {}", this.height, lakesItem.height);
            this.height = lakesItem.height;

            if (!db.patchItem(this.lakesid, "height", String.valueOf(this.height))){
                logger.warn("Database Item UPDATE failure on attribute = height: lakesid = {}", this.lakesid);
            }
        }

        if (this.weight == null || this.weight != lakesItem.weight) {
            logger.info("Weight Updated: {} -> {}", this.weight, lakesItem.weight);
            this.weight = lakesItem.weight;

            if (!db.patchItem(this.lakesid, "weight", String.valueOf(this.weight))){
                logger.warn("Database Item UPDATE failure on attribute = weight: lakesid = {}", this.lakesid);
            }
        }

        if (this.type == null) {
            logger.info("Type Updated: {} -> {}", this.type, lakesItem.type);
            this.type = lakesItem.type;

            if (!db.patchItem(this.lakesid, "type", String.valueOf(this.type))){
                logger.warn("Database Item UPDATE failure on attribute = type: lakesid = {}", this.lakesid);
            }
        }

        if (this.mpn == null) {
            logger.info("MPN Updated: {} -> {}", this.mpn, lakesItem.mpn);
            this.mpn = lakesItem.mpn;

            if (!db.patchItem(this.lakesid, "mpn", String.valueOf(this.mpn))){
                logger.warn("Database Item UPDATE failure on attribute = mpn: lakesid = {}", this.lakesid);
            }
        }

        if (this.title == null) {
            logger.info("Title Updated: {} -> {}", this.title, lakesItem.title);
            this.title = lakesItem.title;

            if (!db.patchItem(this.lakesid, "title", String.valueOf(this.title))){
                logger.warn("Database Item UPDATE failure on attribute = title: lakesid = {}", this.lakesid);
            }
        }

        if (this.description == null) {
            logger.info("Description Updated: {} -> {}", this.description, lakesItem.description);
            this.description = lakesItem.description;

            if (!db.patchItem(this.lakesid, "description", String.valueOf(this.description))){
                logger.warn("Database Item UPDATE failure on attribute = description: lakesid = {}", this.lakesid);
            }
        }

        if (this.upc == null) {
            logger.info("UPC Updated: {} -> {}", this.upc, lakesItem.upc);
            this.upc = lakesItem.upc;

            if (!db.patchItem(this.lakesid, "upc", String.valueOf(this.upc))){
                logger.warn("Database Item UPDATE failure on attribute = upc: lakesid = {}", this.lakesid);
            }
        }

        if (this.sku == null) {
            logger.info("SKU Updated: {} -> {}", this.sku, lakesItem.sku);
            this.sku = lakesItem.sku;

            if (!db.patchItem(this.lakesid, "sku", String.valueOf(this.sku))){
                logger.warn("Database Item UPDATE failure on attribute = sku: lakesid = {}", this.lakesid);
            }
        }

        if (this.lakes_images == null){
            logger.info("Lakes Image Updated: {} -> {}", this.lakes_images, lakesItem.imageLink);
            this.lakes_images = lakesItem.imageLink;

            if (!db.patchItem(this.lakesid, "lakes_images", String.valueOf(this.lakes_images))){
                logger.warn("Database Item UPDATE failure on attribute = lakes_images: lakesid = {}", this.lakesid);
            }
        }

        if (this.fulfillment == null){
            this.fulfillment = DEFAULT_FULFILLMENT;

            if (!db.patchItem(this.lakesid, "fulfillment", String.valueOf(this.fulfillment))){
                logger.warn("Database Item UPDATE failure on attribute = fulfillment: lakesid = {}", this.lakesid);
            }
        }
    }

    @Override
    public String toString() {
        return "DatabaseItem {\n" +
                "    lakesid=" + lakesid + ",\n" +
                "    width=" + width + ",\n" +
                "    length=" + length + ",\n" +
                "    height=" + height + ",\n" +
                "    weight=" + weight + ",\n" +
                "    type='" + type + "',\n" +
                "    mpn='" + mpn + "',\n" +
                "    title='" + title + "',\n" +
                "    description='" + description + "',\n" +
                "    upc='" + upc + "',\n" +
                "    quantity=" + quantity + ",\n" +
                "    custom_quantity=" + custom_quantity + ",\n" +
                "    sku='" + sku + "',\n" +
                "    updated_at=" + updated_at + ",\n" +
                "    milwaukee_images='" + milwaukee_images + "',\n" +
                "    package_width=" + package_width + ",\n" +
                "    package_length=" + package_length + ",\n" +
                "    package_height=" + package_height + ",\n" +
                "    package_weight=" + package_weight + ",\n" +
                "    custom_description='" + custom_description + "',\n" +
                "    lakes_images='" + lakes_images + "',\n" +
                "    minimum_price=" + minimum_price + ",\n" +
                "    calculated_price=" + calculated_price + ",\n" +
                "    maximum_price=" + maximum_price + ",\n" +
                "    lakes_price=" + lakes_price + ",\n" +
                "    custom_price=" + custom_price + ",\n" +
                "    fulfillment=" + fulfillment + "\n" +
                '}';
    }

}
