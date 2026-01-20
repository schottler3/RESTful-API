package dev.lucasschotttler.database;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class Databasing {

    private static final Logger logger = LoggerFactory.getLogger(Databasing.class);    

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
      
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
            return jdbcTemplate.queryForList("SELECT * FROM superior LIMIT ?", limit);
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
        Set<String> numericColumns = Set.of("width", "length", "height", "weight", "quantity", "lakesid");
        Set<String> allowedColumns = Set.of(
            "width", "length", "height", "weight", "type", "mpn", "title", 
            "description", "upc", "brand", "quantity", "sku", "name"
        );
        
        if (!allowedColumns.contains(attribute)) {
            logger.warn("Invalid attribute: {}", attribute);
            return false;
        }
        
        String sql = "UPDATE superior SET " + attribute + " = ?, updated_at = CURRENT_TIMESTAMP WHERE lakesid = ?";
        
        try {
            int rowsAffected;
            
            if (numericColumns.contains(attribute)) {
                if (attribute.equals("quantity") || attribute.equals("lakesid")) {
                    rowsAffected = jdbcTemplate.update(sql, Integer.parseInt(data), lakesid);
                } else {
                    rowsAffected = jdbcTemplate.update(sql, Double.parseDouble(data), lakesid);
                }
            } else {
                rowsAffected = jdbcTemplate.update(sql, data, lakesid);
            }
            
            return rowsAffected > 0;
            
        } catch (NumberFormatException e) {
            logger.error("Invalid number format for attribute {}: {}", attribute, data);
            return false;
        }
    }

    public List<String> getImages(String SKU) {
        String sql = "SELECT milwaukee_images FROM superior WHERE sku LIKE ?";
        String pattern = "%" + SKU + "%";
        return jdbcTemplate.queryForList(sql, String.class, pattern);
    }

}