package dev.lucasschottler.database.queries;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ErrorQueries {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorQueries.class); 
    private final JdbcTemplate db;

    public ErrorQueries(JdbcTemplate db){
        this.db = db;
    }

    public List<Error> getErrors(UUID batch_id) {
        return db.query("SELECT * FROM error WHERE batch_id = ?", ERROR_ROW_MAPPER, batch_id);
    }

    public List<Error> getErrors(String sku){
        return db.query("SELECT * FROM error WHERE sku ILIKE ?", ERROR_ROW_MAPPER, sku);
    }

    public boolean addError(UUID batch_id, String sku, String error_message){
        try{
            String sql = "INSERT INTO error (batch_id, sku, error_message) VALUES (?, ?, ?)";

            return db.update(sql, batch_id, sku, error_message) > 0;
        } catch (Exception e) {
            logger.error("Error adding Error item with data, batch_id {}: sku: {} error_message: {}", batch_id, sku, error_message);
            return false;
        }
    }

    private static final RowMapper<Error> ERROR_ROW_MAPPER = (rs, rowNum) -> {
        Error error = new Error();
        return error;
    };

}
