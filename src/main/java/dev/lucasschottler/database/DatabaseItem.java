package dev.lucasschottler.database;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.lucasschottler.api.controllers.BaseController;
import dev.lucasschottler.api.square.Square;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.update.Amazon;

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
    public String lakes_images;
    public Double minimum_price;
    public Double calculated_price;
    public Double maximum_price;
    public Double lakes_price;
    public Double custom_price;
    public Integer fulfillment;
    public String square_variation_id;
    public String barcode_title;
    public String marketplaces;
    public Timestamp last_amazon;
    public Timestamp last_ebay;

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);
    private static final Square square = new Square();

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
        this.lakes_images = (String) item.get("lakes_images");
        this.minimum_price = (Double) item.get("minimum_price");
        this.calculated_price = (Double) item.get("calculated_price");
        this.maximum_price = (Double) item.get("maximum_price");
        this.lakes_price = (Double) item.get("lakes_price");
        this.custom_price = (Double) item.get("custom_price");
        this.fulfillment = (Integer) item.get("fulfillment");
        this.square_variation_id = (String) item.get("square_variation_id");
        this.barcode_title = (String) item.get("barcode_title"); 
        this.marketplaces = (String) item.get("marketplaces");
        this.last_amazon = (Timestamp) item.get("last_amazon");
        this.last_ebay = (Timestamp) item.get("last_ebay");
    }

    public DatabaseItem(LakesItem item) {
        this.lakesid = item.lakesid;
        this.sku = item.sku;
        this.quantity = item.quantity;
        this.lakes_price = item.price;
        this.width = item.width;
        this.length = item.length;
        this.height = item.height;
        this.weight = item.weight;
        this.type = item.type;
        this.mpn = item.mpn;
        this.title = item.title;
        this.description = item.description;
        this.upc = item.upc;
        this.lakes_images = item.imageLink;
        this.fulfillment = DEFAULT_FULFILLMENT;

        HashMap<String, Double> amazonPrices = Amazon.getPrices(this.lakes_price);
        this.minimum_price = amazonPrices.get("minimum_price");
        this.calculated_price = amazonPrices.get("middle_price");
        this.maximum_price = amazonPrices.get("maximum_price");
    }

    public void updateItem(Databasing db) {
        Integer squareQuantity = square.getInventoryCountByMpn(this.mpn);

        logger.info("DatabaseItem: SquareQuantity = {}. sku = {}", squareQuantity, this.sku);

        //TODO: Make the custom quantity dependant on the bom ratio

        if (squareQuantity != null && !squareQuantity.equals(this.custom_quantity)) {
            logger.info("Custom Quantity (Square) Updated: {} -> {}", this.custom_quantity, squareQuantity);
            this.custom_quantity = squareQuantity;
            if (!db.patchItem(this.sku, "custom_quantity", squareQuantity)) {
                logger.warn("Database Item UPDATE failure on attribute = custom_quantity: sku = {}", this.sku);
            }
        }

        if (this.fulfillment == null) {
            this.fulfillment = DEFAULT_FULFILLMENT;
            if (!db.patchItem(this.sku, "fulfillment", this.fulfillment)) {
                logger.warn("Database Item UPDATE failure on attribute = fulfillment: sku = {}", this.sku);
            }
        }

        if (this.square_variation_id == null) {
            //logger.info("Database Item Updating square_variation_id");
            try {
                this.square_variation_id = square.getVariationID(sku);
                //logger.info("Database Item Updated square_variation_id to {}", this.square_variation_id);
                if (!db.patchItem(this.sku, "square_variation_id", this.square_variation_id)) {
                    logger.warn("Database Item UPDATE failure on attribute = square_variation_id: sku = {}", this.sku);
                }
            } catch (Exception e) {
                logger.error("Database update was unable to get the variation ID! {}", sku);
            }
        }
    }

    public void updateItemUsingLakes(LakesItem lakesItem, Databasing db) {

        if (this.quantity == null || !this.quantity.equals(lakesItem.quantity)) {
            //logger.info("Quantity Updated: {} -> {}", this.quantity, lakesItem.quantity);
            this.quantity = lakesItem.quantity;
            if (!db.patchItem(this.sku, "quantity", this.quantity)) {
                logger.warn("Database Item UPDATE failure on attribute = quantity: sku = {}", this.sku);
            }
        }

        if (this.lakes_price == null || !this.lakes_price.equals(lakesItem.price)) {
            //logger.info("Price Updated: {} -> {}", this.lakes_price, lakesItem.price);
            this.lakes_price = lakesItem.price;
            if (!db.patchItem(this.sku, "lakes_price", this.lakes_price)) {
                logger.warn("Database Item UPDATE failure on attribute = lakes_price: sku = {}", this.sku);
            }
            setPricingFields(this.custom_price != null && this.custom_price > 0 ? this.custom_price : this.lakes_price, db);
        }

        if (this.width == null) {
            this.width = lakesItem.width;
            if (!db.patchItem(this.sku, "width", this.width)) {
                logger.warn("Database Item UPDATE failure on attribute = width: sku = {}", this.sku);
            }
        }

        if (this.length == null) {
            this.length = lakesItem.length;
            if (!db.patchItem(this.sku, "length", this.length)) {
                logger.warn("Database Item UPDATE failure on attribute = length: sku = {}", this.sku);
            }
        }

        if (this.height == null) {
            this.height = lakesItem.height;
            if (!db.patchItem(this.sku, "height", this.height)) {
                logger.warn("Database Item UPDATE failure on attribute = height: sku = {}", this.sku);
            }
        }

        if (this.weight == null) {
            this.weight = lakesItem.weight;
            if (!db.patchItem(this.sku, "weight", this.weight)) {
                logger.warn("Database Item UPDATE failure on attribute = weight: sku = {}", this.sku);
            }
        }

        if (this.type == null) {
            this.type = lakesItem.type;
            if (!db.patchItem(this.sku, "type", this.type)) {
                logger.warn("Database Item UPDATE failure on attribute = type: sku = {}", this.sku);
            }
        }

        if (this.mpn == null) {
            this.mpn = lakesItem.mpn;
            if (!db.patchItem(this.sku, "mpn", this.mpn)) {
                logger.warn("Database Item UPDATE failure on attribute = mpn: sku = {}", this.sku);
            }
        }

        if (this.title == null) {
            this.title = lakesItem.title;
            if (!db.patchItem(this.sku, "title", this.title)) {
                logger.warn("Database Item UPDATE failure on attribute = title: sku = {}", this.sku);
            }
        }

        if (this.description == null) {
            this.description = lakesItem.description;
            if (!db.patchItem(this.sku, "description", this.description)) {
                logger.warn("Database Item UPDATE failure on attribute = description: sku = {}", this.sku);
            }
        }

        if (this.upc == null) {
            this.upc = lakesItem.upc;
            if (!db.patchItem(this.sku, "upc", this.upc)) {
                logger.warn("Database Item UPDATE failure on attribute = upc: sku = {}", this.sku);
            }
        }

        if (this.lakes_images == null) {
            this.lakes_images = lakesItem.imageLink;
            if (!db.patchItem(this.sku, "lakes_images", this.lakes_images)) {
                logger.warn("Database Item UPDATE failure on attribute = lakes_images: sku = {}", this.sku);
            }
        }
    }

    public void setPricingFields(Double price, Databasing db){
        //minimum_price, middle_price, maximum_price
        HashMap<String,Double> amazonPrices = Amazon.getPrices(price);

        this.minimum_price = amazonPrices.get("minimum_price");
        this.calculated_price = amazonPrices.get("middle_price");
        this.maximum_price = amazonPrices.get("maximum_price");

        if (!db.patchItem(this.sku, "minimum_price", String.valueOf(this.minimum_price))){
            logger.warn("Database Item UPDATE failure on attribute = minimum_price: sku = {}", this.sku);
        }
        if (!db.patchItem(this.sku, "calculated_price", String.valueOf(this.calculated_price))){
            logger.warn("Database Item UPDATE failure on attribute = calculated_price: sku = {}", this.sku);
        }
        if (!db.patchItem(this.sku, "maximum_price", String.valueOf(this.maximum_price))){
            logger.warn("Database Item UPDATE failure on attribute maximum_price: sku = {}", this.sku);
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
                "    lakes_images='" + lakes_images + "',\n" +
                "    minimum_price=" + minimum_price + ",\n" +
                "    calculated_price=" + calculated_price + ",\n" +
                "    maximum_price=" + maximum_price + ",\n" +
                "    lakes_price=" + lakes_price + ",\n" +
                "    custom_price=" + custom_price + ",\n" +
                "    fulfillment=" + fulfillment + "\n" +
                "    square_variation_id=" + square_variation_id + "\n" +
                "    barcode_title=" + barcode_title + "\n" +
                "    marketplaces=" + marketplaces + "\n" +
                "    last_amazon=" + last_amazon + "\n" +
                "    last_ebay=" + last_ebay + "\n" +
                '}';
    }

}
