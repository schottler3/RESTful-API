package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import dev.lucasschottler.api.square.Square;
import dev.lucasschottler.api.update.Actions;
import dev.lucasschottler.database.queries.BomQueries;
import dev.lucasschottler.database.queries.DatabaseItemQueries;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.marketplaces.Amazon;
import dev.lucasschottler.marketplaces.Ebay;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
    public Integer square_quantity;
    public String sku;
    public Timestamp updated_at;
    public String images;
    public Double package_width;
    public Double package_length;
    public Double package_height;
    public Double package_weight;
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
    public String ebay_listing_id;
    public UUID batch_id;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseItem.class);
    private static final Square square = new Square();
    private static final Ebay ebay = new Ebay();

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
        this.fulfillment = DEFAULT_FULFILLMENT;

        HashMap<String, Double> amazonPrices = Amazon.getPrices(this.lakes_price);
        this.minimum_price = amazonPrices.get("minimum_price");
        this.calculated_price = amazonPrices.get("middle_price");
        this.maximum_price = amazonPrices.get("maximum_price");
    }

    public void updateItem(DatabaseItemQueries db, BomQueries bq, UUID batch_id, Actions actions) {

        this.batch_id = batch_id;

        if (this.square_variation_id == null || this.square_variation_id.isBlank()) {
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

        if(this.ebay_listing_id == null || this.ebay_listing_id.isBlank()){
            String listingId = ebay.getOffer(this.sku).get(0).getListingId();
            if(listingId != null){
                if(!db.patchItem(this.sku, "ebay_listing_id", listingId)){
                    logger.warn("Database Item UPDATE failure on attribute = ebay_listing_id: sku = {}", this.sku);
                }
            } else {
                logger.warn("Database update on item's ebay_listing_id was null from ebay!, {}", this.sku);
            }
        }

        setClampPrice(bq, db, batch_id, actions);

        int quantity = getClampQuantity(db, bq, actions, batch_id);

        if (this.custom_quantity == null || this.custom_quantity != quantity) {
            logger.info("Custom Quantity (Square) Updated: {} -> {}", this.custom_quantity, quantity);
            this.custom_quantity = quantity;
            if (!db.patchItem(this.sku, "custom_quantity", quantity)) {
                logger.warn("Database Item UPDATE failure on attribute = custom_quantity: sku = {}", this.sku);
            }
        }

    }

    public void updateItemUsingLakes(LakesItem lakesItem, DatabaseItemQueries db) {

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
    }

    public void setPricingFields(Double price, DatabaseItemQueries db){
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

    public int getClampQuantity(DatabaseItemQueries db, BomQueries bomQueries, Actions actions, UUID batchId){
        logger.info("DatabaseItem: Getting clampedQuantity from item: {}", this.sku );
        List<Bom> bomData = bomQueries.getBom(this.mpn);

        if(bomData != null && !bomData.isEmpty()){
            logger.info("DatabaseItem: Found bomData {}", this.sku );
            int clampQuantity = 0;

            for(Bom bomItem : bomData){
                logger.info("DatabaseItem: Bom item: {} of: {}", bomItem, this.sku );

                UUID parentBatchId = db.getBatchId(bomItem.parent_sku);

                if(parentBatchId == null || !parentBatchId.equals(batchId)){
                    logger.info("DatabaseItem-getClampQuantity: Going one deeper on item: {}", bomItem.parent_sku);
                    actions.updateItem(bomItem.parent_sku);
                }

                DatabaseItem parentItem = db.getData(bomItem.parent_sku);
                int quantity = (int) Math.floor(parentItem.getClampQuantity(db, bomQueries, actions, batchId) * bomItem.ratio);

                if(quantity < clampQuantity){
                    clampQuantity = quantity;
                    logger.info("DatabaseItem: New clamp quantity found {}", clampQuantity);
                }
            }

            return clampQuantity;

        }

        if(this.square_variation_id != null && !this.square_variation_id.isBlank()){
            Integer squareInventory = square.getInventoryCountByMpn(this.mpn);

            if(squareInventory != null){                
                logger.info("DatabaseItem: Square inventory exists: {} for: {}", squareInventory, this.sku );
                return squareInventory;
            } else {
                logger.info("DatabaseItem: Square inventory cant be reached! {}", this.sku);
            }
        }
        logger.info("DatabaseItem: No square inventory found and no bomData quantities for: {} using the existing custom_quantity: {} or quantity: {}", this.sku, this.custom_quantity, this.quantity );

        return this.custom_quantity != null && this.custom_quantity > 0 ? this.custom_quantity : this.quantity;
    }

    public double getQuantityToUse(){
        if(this.custom_quantity != null && this.custom_quantity > 0){
            return this.custom_quantity;
        } else if(this.square_quantity != null && this.square_quantity > 0){
            return this.square_quantity;
        } else if(this.quantity != null && this.quantity > 0){
            return this.quantity * .66;
        } else {
            return 0;
        }
    }

    public int getFulfillmentTime(){
        if(this.fulfillment != null && this.fulfillment > 0){
            return this.fulfillment;
        } else if(this.custom_quantity != null && this.custom_quantity > 0 || this.square_quantity != null && this.square_quantity > 0){
            return 1;
        } else {
            return 5;
        }
    }

    public void setClampPrice(BomQueries bomQueries, DatabaseItemQueries db, UUID batchId, Actions actions){
        Double bulkSplitPrice = 0.0;
        List<Bom> bomData = bomQueries.getBom(this.mpn);

        if(bomData != null && !bomData.isEmpty()){

            for (Bom bomItem : bomData){
                String parent_sku = bomItem.parent_sku;
                if(parent_sku != null){

                    UUID parentBatchId = db.getBatchId(parent_sku);

                    if(parentBatchId == null || !db.getBatchId(parent_sku).equals(batchId)){
                        actions.updateItem(parent_sku);
                    }

                    DatabaseItem parentDbItem;

                    try {
                        parentDbItem = db.getData(parent_sku);
                    } catch (EmptyResultDataAccessException e) {
                        logger.warn("Actions - Bom: dependency not found in database: {}", parent_sku);
                        continue;
                    }

                    Double lakes_price = parentDbItem.lakes_price;
                    Double ratio = bomItem.ratio;

                    if(ratio != null){
                        if(lakes_price != null && lakes_price > 0){
                            bulkSplitPrice += lakes_price * ratio;
                        }
                    }
                }
            }
        }

        if(bulkSplitPrice != null && bulkSplitPrice > 0){
            this.setPricingFields(bulkSplitPrice, db);
        }
    }
    
    public DatabaseItem getParentData(BomQueries bomQueries, DatabaseItemQueries db){
        List<Bom> bomData = bomQueries.getBom(this.mpn);

        if(bomData != null && bomData.size() > 0){
            String child_sku = bomData.get(0).child_sku;

            if(child_sku != null){
                return db.getData(child_sku);
            }
        }

        return null;
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
                "    square_quantity=" + square_quantity + ",\n" +
                "    sku='" + sku + "',\n" +
                "    updated_at=" + updated_at + ",\n" +
                "    milwaukee_images='" + images + "',\n" +
                "    package_width=" + package_width + ",\n" +
                "    package_length=" + package_length + ",\n" +
                "    package_height=" + package_height + ",\n" +
                "    package_weight=" + package_weight + ",\n" +
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
                "    batch_id=" + batch_id + "\n" +
                '}';
    }

}
