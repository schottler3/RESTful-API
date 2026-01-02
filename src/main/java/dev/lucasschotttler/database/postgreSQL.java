package dev.lucasschotttler.database;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class postgreSQL {
    
    private final JdbcTemplate jdbcTemplate;
    
    public postgreSQL(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // Example query method
    public String testConnection() {
        return jdbcTemplate.queryForObject("SELECT version()", String.class);
    }
    
    // Example: Get data from a table
    public List<String> getImages(String tableName, String SKU) {
        return jdbcTemplate.queryForList(
            "SELECT web_api FROM " + tableName + " WHERE filename LIKE '%" + SKU + "%';", 
            String.class
        );
    }
}