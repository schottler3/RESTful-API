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
    public List<Map<String, Object>> getData(String tableName) {
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }
}