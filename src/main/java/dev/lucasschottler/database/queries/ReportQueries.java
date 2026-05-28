package dev.lucasschottler.database.queries;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.lucasschottler.database.tableData.DatabaseItem;
import dev.lucasschottler.lakes.LakesItem;

@Repository
public class ReportQueries {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportQueries.class); 
    private final JdbcTemplate db;

    public ReportQueries(JdbcTemplate db){
        this.db = db;
    }

    public boolean addReportNewItem(LakesItem item) {
        String sql = "INSERT INTO report (lakesid, title, description, sku, lakes_price, images, quantity, date_added, type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'new') ON CONFLICT (lakesid) DO NOTHING";
        return db.update(sql, item.lakesid, item.title, item.description, item.sku, item.price, item.imageLink, item.quantity, Timestamp.valueOf(LocalDateTime.now())) > 0;
    }

    public boolean addReportDiscItem(DatabaseItem item) {
        String sql = "INSERT INTO report (lakesid, title, description, sku, lakes_price, lakes_images, quantity, date_added, type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'disc') ON CONFLICT (lakesid) DO NOTHING";
        return db.update(sql, item.lakesid, item.title, item.description, item.sku, item.lakes_price, item.images, item.quantity, Timestamp.valueOf(LocalDateTime.now())) > 0;
    }

    public boolean deleteReportItem(int lakesid){
        String sql = "DELETE FROM report WHERE lakesid=?";

        return db.update(sql,lakesid) > 0;
    }

    public List<Map<String,Object>> getReport(String type){

        String sql = "SELECT * FROM report WHERE type=? ORDER BY sku ASC";

        return db.queryForList(sql, type);
    }

    public Map<String, Object> getReport(int lakesid, String type) {
        String sql = "SELECT * FROM report WHERE lakesid = ? AND type = ? LIMIT 1";
        try {
            return db.queryForMap(sql, lakesid, type);
        } catch (EmptyResultDataAccessException e) {
            return null; 
        }
    }

    public List<Integer> getAllReportIds(String type){

        String sql = "SELECT lakesid FROM report WHERE type=?";

        return db.queryForList(sql, Integer.class, type);

    }

    public List<Map<String,Object>> checkReportForExistingSku(String sku) {

        if (sku == null || sku.isBlank()) {
            return List.of();
        }

        String safeSku = sku.replace("%", "\\%").replace("_", "\\_");
        String sql = "SELECT * FROM report WHERE sku ILIKE ?";

        return db.queryForList(sql, safeSku);
    }
}
