package dev.lucasschottler.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import dev.lucasschottler.lakes.LakesItem;
import dev.lucasschottler.update.Amazon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;


@Service
public class Databasing {

    private static final Logger logger = LoggerFactory.getLogger(Databasing.class);   

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
        // String columns
        "type", "mpn", "title", "description", "upc", "sku",
        "milwaukee_images", "lakes_images"
    );

    private final JdbcTemplate jdbcTemplate;
      
    public Databasing(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public java.util.Map<String, Object> getData(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            String sql = "SELECT * FROM superior ORDER BY sku ASC";
            return jdbcTemplate.queryForMap(sql);
        }
        String sql = "SELECT * FROM superior WHERE sku LIKE ? ORDER BY sku ASC";
        return jdbcTemplate.queryForMap(sql, sku);
    }

    public java.util.Map<String, Object> getData(int lakesid) {
        String sql = "SELECT * FROM superior WHERE lakesid LIKE ?";
        return jdbcTemplate.queryForMap(sql, lakesid);
    }

    public int getLastLakesId() {
        String superiorSQL = "SELECT lakesid FROM superior ORDER BY lakesid DESC LIMIT 1";
        String reportSQL = "SELECT lakesid FROM report ORDER BY lakesid ASC LIMIT 1";

        Integer lakesid = null;
        Integer reportLakesId = null;

        try {
            lakesid = jdbcTemplate.queryForObject(superiorSQL, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            logger.info("No rows in superior table");
        }

        try {
            reportLakesId = jdbcTemplate.queryForObject(reportSQL, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            logger.info("No rows in report table");
        }

        if (lakesid != null && reportLakesId != null) {
            return Math.min(lakesid, reportLakesId);
        } else if (lakesid != null) {
            return lakesid;
        } else if (reportLakesId != null) {
            return reportLakesId;
        } else {
            return -1;
        }
    }

    public List<Map<String, Object>> queryDatabase(String query, int limit, String time) {

        String order = "sku ASC";

        if(time != null){
            if(time.equals("newest")){
                order = "updated_at DESC";
            }
            else if(time.equals("oldest")){
                order = "updated_at ASC";
            }
        }
        
        // Handle empty query - return all results with ordering
        if (query == null || query.trim().isEmpty()) {
            // Must use string concatenation for ORDER BY and LIMIT
            String sql = "SELECT * FROM superior ORDER BY " + order + " LIMIT " + limit;
            return jdbcTemplate.queryForList(sql);
        }
        
        // Try exact SKU match first
        String exactMatchSql = "SELECT * FROM superior WHERE sku = ? ORDER BY " + order + " LIMIT " + limit;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(exactMatchSql, query);
        
        if(!result.isEmpty()){
            return result;
        }
        
        // Fall back to fuzzy search across all fields
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

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? new ArrayList<>() : results;
    }

    public boolean patchItem(String sku, String attribute, String data) {
    
        if (!allowedColumns.contains(attribute)) {
            logger.warn("Invalid attribute: {}", attribute);
            return false;
        }
        
        String sql = "UPDATE superior SET " + attribute + " = ?, updated_at = CURRENT_TIMESTAMP WHERE sku = ?";
        logger.info("DEBUG - SQL: {}", sql);
        
        try {
            int rowsAffected;

            if (data == null || data.equalsIgnoreCase("null") || data.trim().isEmpty()) {
                int sqlType = Types.VARCHAR;
                if (integerColumns.contains(attribute)) sqlType = Types.INTEGER;
                else if (doubleColumns.contains(attribute)) sqlType = Types.DOUBLE;

                rowsAffected = jdbcTemplate.update(sql, new Object[]{null, sku}, new int[]{sqlType, Types.INTEGER});
            } else if (integerColumns.contains(attribute)) {
                rowsAffected = jdbcTemplate.update(sql, Integer.parseInt(data), sku);
            } else if (doubleColumns.contains(attribute)) {
                rowsAffected = jdbcTemplate.update(sql, Double.parseDouble(data), sku);
            } else {
                rowsAffected = jdbcTemplate.update(sql, data, sku);
            }

            logger.info("Databasing patchItem rowsAffected: {}", rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (NumberFormatException e) {
            logger.error("Invalid number format for attribute {}: {}", attribute, data);
            return false;
        } catch (Exception e) {
            logger.error("Error updating attribute {}: {}", attribute, e.getMessage(), e);
            return false;
        }
    }

    public Integer createItem(DatabaseItem dbItem) {
        logger.info("Databasing: Creating new item with sku: {}", dbItem.sku);

        String sql = """
            INSERT INTO superior (
                lakesid, width, length, height, weight, type, mpn, title, description,
                upc, quantity, custom_quantity, sku, milwaukee_images, package_width,
                package_length, package_height, package_weight, lakes_images,
                minimum_price, calculated_price, maximum_price, lakes_price,
                custom_price, fulfillment, square_variation_id
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            ) RETURNING id
        """;

        try {
            Integer id = jdbcTemplate.queryForObject(sql, Integer.class,
                dbItem.lakesid, dbItem.width, dbItem.length, dbItem.height, dbItem.weight,
                dbItem.type, dbItem.mpn, dbItem.title, dbItem.description, dbItem.upc,
                dbItem.quantity, dbItem.custom_quantity, dbItem.sku, dbItem.milwaukee_images,
                dbItem.package_width, dbItem.package_length, dbItem.package_height, dbItem.package_weight,
                dbItem.lakes_images, dbItem.minimum_price, dbItem.calculated_price, dbItem.maximum_price,
                dbItem.lakes_price, dbItem.custom_price, dbItem.fulfillment, dbItem.square_variation_id
            );

            logger.info("Databasing: createItem created with id: {}", id);
            return id;

        } catch (Exception e) {
            logger.error("Databasing: createItem failed for sku {}: {}", dbItem.sku, e.getMessage());
            return null;
        }
    }

    public boolean deleteItem(String sku) {
        
        String sql = "DELETE FROM superior WHERE sku=?;";

        int rows = jdbcTemplate.update(sql, sku);

        if(rows > 0){
            return true;
        }
        else {
            return false;
        }

    }

    public boolean updateCustomQuantity(String sku, int quantity){

        logger.info("Databasing: Updating Custom quantity on sku = {} with q = {}", sku, quantity);

        String sql = "UPDATE superior SET custom_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE sku = ?";

        if(jdbcTemplate.update(sql, quantity, sku) > 0){
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
                lakes_images = ?,
                minimum_price = ?,
                calculated_price = ?,
                maximum_price = ?,
                lakes_price = ?,
                fulfillment = 5
            WHERE lakesid = ?
        """;
        
        try {
            int rowsAffected = jdbcTemplate.update(sql,
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

    public List<String> getImages(String SKU) {
        String sql = "SELECT milwaukee_images FROM superior WHERE sku LIKE ?";
        String pattern = "%" + SKU + "%";
        return jdbcTemplate.queryForList(sql, String.class, pattern);
    }

    public List<java.util.Map<String, Object>> getBom(String parent_sku){
        String sql = "SELECT * FROM bom WHERE parent_sku = ?";

        return jdbcTemplate.queryForList(sql, parent_sku);
    }

    public boolean addBom(String parent_sku, String child_sku, Double quantity){

        logger.info("Adding BOM item to parent {}: {} q: {}", parent_sku, child_sku, quantity);

        try{
            String sql = "INSERT INTO bom (parent_sku, child_sku, quantity) VALUES (?, ?, ?) " +
                        "ON CONFLICT (parent_sku, child_sku) " +
                        "DO UPDATE SET quantity = EXCLUDED.quantity";

            return jdbcTemplate.update(sql, parent_sku, child_sku, quantity) > 0;
        } catch (Exception e) {
            logger.error("Error adding BOM item to parent {}: {} q: {} e: {}", parent_sku, child_sku, quantity, e);
            return false;
        }
    }

    public int removeBom(String parent_sku, String child_sku){
        logger.info("Removing BOM item to parent {}: {}", parent_sku, child_sku);

        try{
            String sql = "DELETE FROM bom WHERE parent_sku = ? AND child_sku = ?";
            return jdbcTemplate.update(sql, parent_sku, child_sku);
        } catch (Exception e) {
            logger.error("Error removing BOM item to parent {}: {} e: {}", parent_sku, child_sku, e);
            return -1;
        }
    }

    public boolean createAlt(DatabaseItem dbItem, String parentSku) {

        logger.info("Databasing: Creating new alt item with sku: {}", dbItem.sku);

        String sql = """
            INSERT INTO superior (
                lakesid, width, length, height, weight, type, mpn, title, description,
                upc, quantity, custom_quantity, sku, milwaukee_images, package_width,
                package_length, package_height, package_weight, lakes_images,
                minimum_price, calculated_price, maximum_price, lakes_price,
                custom_price, fulfillment, square_variation_id, parent_sku
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """;

        try {
            int rows = jdbcTemplate.update(sql,
                dbItem.lakesid, dbItem.width, dbItem.length, dbItem.height, dbItem.weight,
                dbItem.type, dbItem.mpn, dbItem.title, dbItem.description, dbItem.upc,
                dbItem.quantity, dbItem.custom_quantity, dbItem.sku, dbItem.milwaukee_images,
                dbItem.package_width, dbItem.package_length, dbItem.package_height, dbItem.package_weight,
                dbItem.lakes_images, dbItem.minimum_price, dbItem.calculated_price, dbItem.maximum_price,
                dbItem.lakes_price, dbItem.custom_price, dbItem.fulfillment, dbItem.square_variation_id, parentSku
            );

            logger.info("Databasing: Alt item created with rows: {}", rows);
            return rows > 0;

        } catch (Exception e) {
            logger.error("Databasing: Alt item creation failed for sku {}: {}", dbItem.sku, e.getMessage());
            return false;
        }
    }

    public List<java.util.Map<String,Object>> getAlts(String parentSku){
        logger.info("Databasing received request for all alternatives for parentSku: {}", parentSku);
        
        String sql = "SELECT * FROM superior WHERE parent_sku = ?;";

        List<java.util.Map<String,Object>> alternatives = jdbcTemplate.queryForList(sql,parentSku);

        logger.info("Databasing retrieved all alternatives for parentSku: {} - {} ", parentSku, alternatives.toString());

        return alternatives;
    }

    public boolean addReportItem(LakesItem item) {
        String sql = "INSERT INTO report (lakesid, title, description, sku, lakes_price, lakes_images, quantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql, item.lakesid, item.title, item.description, item.sku, item.price, item.imageLink, item.quantity) > 0;
    }

    public List<Map<String,Object>> getReport(){

        String sql = "SELECT * FROM report ORDER BY sku ASC";

        return jdbcTemplate.queryForList(sql);
    }
}