package dev.lucasschottler.database.queries;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.lucasschottler.database.tableData.DatabaseItem;
import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.marketplaces.Amazon;

@Repository
public class DatabaseItemQueries {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseItemQueries.class); 
    private final JdbcTemplate db;

    private static final Set<String> integerColumns = Set.of("quantity", "custom_quantity", "fulfillment");

    private static final Set<String> doubleColumns = Set.of(
        "width", "length", "height", "weight",
        "package_width", "package_length", "package_height", "package_weight",
        "minimum_price", "calculated_price", "maximum_price", "lakes_price", "custom_price"
    );

    private static final Set<String> allowedColumns = Set.of(
        // Integer columns
        "quantity", "custom_quantity", "fulfillment",
        // Double columns
        "width", "length", "height", "weight",
        "package_width", "package_length", "package_height", "package_weight",
        "minimum_price", "calculated_price", "maximum_price", "lakes_price", "custom_price",
        "bulk_split_price",
        // String columns
        "type", "mpn", "title", "description", "upc", "sku",
        "images", "barcode_title", "marketplaces", "square_variation_id", "ebay_listing_id"
    );

    @Autowired
    public DatabaseItemQueries(JdbcTemplate db){
        this.db = db;
    }

    private static final RowMapper<DatabaseItem> DATABASE_ITEM_MAPPER = (rs, rowNum) -> {
        DatabaseItem item = new DatabaseItem();
        item.setLakesid(rs.getObject("lakesid", Integer.class));
        item.setWidth(rs.getObject("width", Double.class));
        item.setLength(rs.getObject("length", Double.class));
        item.setHeight(rs.getObject("height", Double.class));
        item.setWeight(rs.getObject("weight", Double.class));
        item.setType(rs.getString("type"));
        item.setMpn(rs.getString("mpn"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setUpc(rs.getString("upc"));
        item.setQuantity(rs.getObject("quantity", Integer.class));
        item.setCustom_quantity(rs.getObject("custom_quantity", Integer.class));
        item.setSku(rs.getString("sku"));
        item.setUpdated_at(rs.getTimestamp("updated_at"));
        item.setImages(rs.getString("images"));
        item.setPackage_width(rs.getObject("package_width", Double.class));
        item.setPackage_length(rs.getObject("package_length", Double.class));
        item.setPackage_height(rs.getObject("package_height", Double.class));
        item.setPackage_weight(rs.getObject("package_weight", Double.class));
        item.setMinimum_price(rs.getObject("minimum_price", Double.class));
        item.setCalculated_price(rs.getObject("calculated_price", Double.class));
        item.setMaximum_price(rs.getObject("maximum_price", Double.class));
        item.setLakes_price(rs.getObject("lakes_price", Double.class));
        item.setCustom_price(rs.getObject("custom_price", Double.class));
        item.setFulfillment(rs.getObject("fulfillment", Integer.class));
        item.setSquare_variation_id(rs.getString("square_variation_id"));
        item.setBarcode_title(rs.getString("barcode_title"));
        item.setMarketplaces(rs.getString("marketplaces"));
        item.setLast_amazon(rs.getTimestamp("last_amazon"));
        item.setLast_ebay(rs.getTimestamp("last_ebay"));
        item.setEbay_listing_id(rs.getString("ebay_listing_id"));
        return item;
    };

    public DatabaseItem getData(String sku) {
        String sql = "SELECT * FROM superior WHERE sku = ? LIMIT 1";
        try {
            return db.queryForObject(sql, DATABASE_ITEM_MAPPER, sku);
        } catch (Exception e) {
            logger.info("Databasing: Could not find superior item with sku: {}", sku);
            return null;
        }
    }

    public DatabaseItem getData(int lakesid) {
        String sql = "SELECT * FROM superior WHERE lakesid LIKE ? LIMIT 1";
        try {
            return db.queryForObject(sql, DATABASE_ITEM_MAPPER, lakesid);
        } catch (Exception e) {
            logger.info("Databasing: Could not find superior item with lakesid: {}", lakesid);
            return null;
        }
    }

    public List<DatabaseItem> queryDatabase(String query, int limit, String time) {

        String order = "sku ASC";

        if(time != null){
            if(time.equals("newest")){
                order = "updated_at DESC";
            }
            else if(time.equals("oldest")){
                order = "updated_at ASC";
            }
        }
        
        if (query == null || query.trim().isEmpty()) {
            String sql = "SELECT * FROM superior ORDER BY " + order + " LIMIT " + limit;
            return db.query(sql, DATABASE_ITEM_MAPPER);
        }
        
        String exactMatchSql = "SELECT * FROM superior WHERE sku = ? ORDER BY " + order + " LIMIT " + limit;
        List<DatabaseItem> result = db.query(exactMatchSql, DATABASE_ITEM_MAPPER, query);

        if (!result.isEmpty()) {
            return result;
        }

        String likeMatchSql = "SELECT * FROM superior WHERE sku ILIKE ? ORDER BY " + order + " LIMIT " + limit;
        result = db.query(likeMatchSql, DATABASE_ITEM_MAPPER, "%" + query + "%");

        if (!result.isEmpty()) {
            return result;
        }
        
        String sql = "SELECT * FROM superior WHERE" +
                " CAST(lakesid AS TEXT) ILIKE ? OR" +
                " CAST(width AS TEXT) ILIKE ? OR" +
                " CAST(length AS TEXT) ILIKE ? OR" +
                " CAST(height AS TEXT) ILIKE ? OR" +
                " CAST(weight AS TEXT) ILIKE ? OR" +
                " type ILIKE ? OR" +
                " mpn ILIKE ? OR" +
                " title ILIKE ? OR" +
                " description ILIKE ? OR" +
                " upc ILIKE ? OR" +
                " CAST(quantity AS TEXT) ILIKE ? OR" +
                " sku ILIKE ? OR" +
                " CAST(updated_at AS TEXT) ILIKE ?" +
                " ORDER BY " + order + " LIMIT " + limit;

        String pattern = "%" + query + "%";
        Object[] params = new Object[13];
        Arrays.fill(params, 0, 13, pattern);

        List<DatabaseItem> results = db.query(sql, DATABASE_ITEM_MAPPER, params);
        return results.isEmpty() ? new ArrayList<>() : results;
    }

    public List<DatabaseItem> getAlts(String sku, String mpn){
        //logger.info("Databasing received request for all alternatives for parentSku: {}", mpn);
        
        String sql = "SELECT * FROM superior WHERE mpn = ? AND sku != ?;";

        List<DatabaseItem> alternatives = db.query(sql,DATABASE_ITEM_MAPPER, mpn, sku);

       // logger.info("Databasing retrieved all alternatives for parentSku: {} - {} ", mpn, alternatives.toString());

        return alternatives;
    }

    public boolean patchItem(String sku, String attribute, Object data) {

        if (!allowedColumns.contains(attribute)) {
            logger.warn("Invalid attribute: {}", attribute);
            return false;
        }

        String sql = "UPDATE superior SET " + attribute + " = ?, updated_at = CURRENT_TIMESTAMP WHERE sku = ?";
        logger.debug("SQL: {}", sql);

        try {
            int rowsAffected;

            if (data == null) {
                int sqlType = Types.VARCHAR;
                if (integerColumns.contains(attribute)) sqlType = Types.INTEGER;
                else if (doubleColumns.contains(attribute)) sqlType = Types.DOUBLE;
                rowsAffected = db.update(sql, new Object[]{null, sku}, new int[]{sqlType, Types.VARCHAR});

            } else if (integerColumns.contains(attribute)) {
                int value = (data instanceof Integer) ? (Integer) data : Integer.parseInt(data.toString());
                rowsAffected = db.update(sql, value, sku);

            } else if (doubleColumns.contains(attribute)) {
                double value = (data instanceof Double) ? (Double) data : Double.parseDouble(data.toString());
                rowsAffected = db.update(sql, value, sku);

            } else {
                rowsAffected = db.update(sql, data.toString(), sku);
            }

            return rowsAffected > 0;

        } catch (NumberFormatException e) {
            logger.error("Invalid number format for attribute {}: {}", attribute, data);
            return false;
        } catch (Exception e) {
            logger.error("Error updating attribute {}: {}", attribute, e.getMessage(), e);
            return false;
        }
    }

    public boolean createItem(DatabaseItem dbItem, String marketplaces) {
        //logger.info("Databasing: Creating new item with sku: {}", dbItem.sku);

        String sql = """
            INSERT INTO superior (
                lakesid, width, length, height, weight, type, mpn, title, description,
                upc, quantity, custom_quantity, sku, images, package_width,
                package_length, package_height, package_weight,
                minimum_price, calculated_price, maximum_price, lakes_price,
                custom_price, fulfillment, square_variation_id, marketplaces
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """;

        try {
            int updated = db.update(sql,
                dbItem.lakesid, dbItem.width, dbItem.length, dbItem.height, dbItem.weight,
                dbItem.type, dbItem.mpn, dbItem.title, dbItem.description, dbItem.upc,
                dbItem.quantity, dbItem.custom_quantity, dbItem.sku, dbItem.images,
                dbItem.package_width, dbItem.package_length, dbItem.package_height, 
                dbItem.package_weight, dbItem.minimum_price, dbItem.calculated_price, dbItem.maximum_price,
                dbItem.lakes_price, dbItem.custom_price, dbItem.fulfillment, dbItem.square_variation_id, marketplaces
            );

            //logger.info("Databasing: createItem created with sku: {}", dbItem.sku);

            return updated > 0;

        } catch (Exception e) {
            logger.error("Databasing: createItem failed for sku {}: {}", dbItem.sku, e.getMessage());
            return false;
        }
    }

    public boolean createItem(String sku, String marketplaces){
        String sql = "INSERT INTO superior (sku, marketplaces) VALUES (?,?);";

        return db.update(sql, sku, marketplaces) > 0;
    }

    public boolean deleteItem(String sku) {
        
        String sql = "DELETE FROM superior WHERE sku=?;";

        int rows = db.update(sql, sku);

        if(rows > 0){
            return true;
        }
        else {
            return false;
        }

    }

    public boolean updateCustomQuantity(String sku, int quantity){

        //logger.info("Databasing: Updating Custom quantity on sku = {} with q = {}", sku, quantity);

        String sql = "UPDATE superior SET custom_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE sku = ?";

        if(db.update(sql, quantity, sku) > 0){
            logger.info("Databasing: Custom quantity updated successfully for item: {} with quantity: {}", sku, quantity);
            return true;
        }
        else {
            logger.info("Databasing: Custom quantity update failure for item: {} with quantity: {}", sku, quantity);
            return false;
        }
    }

    public boolean resetItem(LakesItem lakesItem) {
        HashMap<String, Double> amazonPrices = Amazon.getPrices(lakesItem.price);
        if (amazonPrices == null) {
            logger.error("Amazon prices could not be retrieved for lakesItem with ID: {}", lakesItem.lakesid);
            return false;
        }
        
        String sql = """
            UPDATE superior
            SET 
                width = ?,
                length = ?,
                height = ?,
                weight = ?,
                type = ?,
                mpn = ?,
                title = ?,
                description = ?,
                upc = ?,
                quantity = ?,
                custom_quantity = NULL,
                sku = ?,
                package_width = NULL,
                package_length = NULL,
                package_height = NULL,
                package_weight = NULL,
                images = ?,
                minimum_price = ?,
                calculated_price = ?,
                maximum_price = ?,
                lakes_price = ?,
                fulfillment = 5
            WHERE lakesid = ?
        """;
        
        try {
            int rowsAffected = db.update(sql,
                lakesItem.width,
                lakesItem.length,
                lakesItem.height,
                lakesItem.weight,
                lakesItem.type,
                lakesItem.mpn,
                lakesItem.title,
                lakesItem.description,
                lakesItem.upc,
                lakesItem.quantity,
                lakesItem.sku,
                lakesItem.imageLink,
                amazonPrices.get("minimum_price"),
                amazonPrices.get("middle_price"),
                amazonPrices.get("maximum_price"),
                lakesItem.price,
                lakesItem.lakesid
            );
            
            return rowsAffected > 0;
        } catch (Exception e) {
            logger.error("Failed to reset item {}: {}", lakesItem.lakesid, e.getMessage());
            return false;
        }
    }
}
