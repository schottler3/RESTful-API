package dev.lucasschottler.database.queries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.lucasschottler.database.tableData.Batch;

@Repository
public class BatchQueries {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchQueries.class); 
    private final JdbcTemplate db;

    public BatchQueries(JdbcTemplate db){
        this.db = db;
    }

    public Batch getBatch(String batch_id) {
        return db.queryForObject("SELECT * FROM batch WHERE batch_id = ? LIMIT 1", BATCH_ROW_MAPPER, batch_id);
    }

    public boolean addBatch(String batch_id){
        try{
            String sql = "INSERT INTO batch (batch_id) VALUES (?)";

            return db.update(sql, batch_id) > 0;
        } catch (Exception e) {
            logger.error("Error adding Error item with data, batch_id {}", batch_id);
            return false;
        }
    }

    private static final RowMapper<Batch> BATCH_ROW_MAPPER = (rs, rowNum) -> {
        Batch batch = new Batch();
        return batch;
    };

}
