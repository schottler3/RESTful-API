package dev.lucasschotttler.database;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class Databasing {

    private static final Logger logger = LoggerFactory.getLogger(Databasing.class);    

    private static final Set<String> integerColumns = Set.of("lakesid", "quantity", "custom_quantity", "fulfillment");

    private static final Set<String> doubleColumns = Set.of(
        "width", "length", "height", "weight",
        "package_width", "package_length", "package_height", "package_weight",
        "minimum_price", "calculated_price", "maximum_price", "lakes_price", "custom_price"
    );

    private static final Set<String> allowedColumns = Set.of(
        // Integer columns
        "lakesid", "quantity", "custom_quantity", "fulfillment",
        // Double columns
        "width", "length", "height", "weight",
        "package_width", "package_length", "package_height", "package_weight",
        "minimum_price", "calculated_price", "maximum_price", "lakes_price", "custom_price",
        // String columns
        "type", "mpn", "title", "description", "upc", "sku",
        "milwaukee_images", "custom_description", "lakes_images"
    );

    private final JdbcTemplate jdbcTemplate;
      
    public Databasing(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<java.util.Map<String, Object>> getData(String SKU, int limit) {
        if (SKU == null || SKU.trim().isEmpty()) {
            String sql = "SELECT * FROM superior ORDER BY lakesid ASC LIMIT ?";
            return jdbcTemplate.queryForList(sql, limit);
        }
        String sql = "SELECT * FROM superior WHERE sku LIKE ? ORDER BY lakesid ASC LIMIT ?";
        String pattern = "%" + SKU + "%";
        return jdbcTemplate.queryForList(sql, pattern, limit);
    }

    public List<java.util.Map<String, Object>> queryDatabase(String query, int limit) {
        
        if (query == null || query.trim().isEmpty()) {
            return jdbcTemplate.queryForList("SELECT * FROM superior ORDER BY lakesid ASC LIMIT ?", limit);
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
                " CAST(updated_at AS TEXT) ILIKE ? ORDER BY lakesid ASC LIMIT ?";

        String pattern = "%" + query + "%";
        Object[] params = new Object[14];
        Arrays.fill(params, 0, 12, pattern);
        params[12] = pattern;
        params[13] = limit;

        List<java.util.Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        if(results.size() <= 0){
            return null;
        }
        else{
            return results;
        }
    }

    public boolean patchItem(Integer lakesid, String attribute, String data) {
        
        if (!allowedColumns.contains(attribute)) {
            logger.warn("Invalid attribute: {}", attribute);
            return false;
        }
        
        String sql = "UPDATE superior SET " + attribute + " = ?, updated_at = CURRENT_TIMESTAMP WHERE lakesid = ?";
        logger.info("DEBUG - SQL: {}", sql);
        
        try {
            int rowsAffected;
            
            if (integerColumns.contains(attribute)) {
                rowsAffected = jdbcTemplate.update(sql, Integer.parseInt(data), lakesid);
            } else if (doubleColumns.contains(attribute)) {
                rowsAffected = jdbcTemplate.update(sql, Double.parseDouble(data), lakesid);
            } else {
                rowsAffected = jdbcTemplate.update(sql, data, lakesid);
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

    public List<String> getImages(String SKU) {
        String sql = "SELECT milwaukee_images FROM superior WHERE sku LIKE ?";
        String pattern = "%" + SKU + "%";
        return jdbcTemplate.queryForList(sql, String.class, pattern);
    }
}