package dev.lucasschottler.database.queries;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.lucasschottler.database.tableData.Bom;

@Repository
public class BomQueries {
    
    private static final Logger logger = LoggerFactory.getLogger(BomQueries.class); 
    private final JdbcTemplate db;

    public BomQueries(JdbcTemplate db){
        this.db = db;
    }

    public List<Bom> getBom(String child_sku) {
        return db.query("SELECT * FROM bom WHERE child_sku = ?", BOM_ROW_MAPPER, child_sku);
    }

    public boolean addBom(String child_sku, String parent_sku, Double ratio){

        //logger.info("Adding BOM item to parent {}: {} q: {}", child_sku, child_sku, ratio);

        try{
            String sql = "INSERT INTO bom (child_sku, parent_sku, ratio) VALUES (?, ?, ?) " +
                        "ON CONFLICT (child_sku, parent_sku) " +
                        "DO UPDATE SET ratio = EXCLUDED.ratio";

            return db.update(sql, child_sku, parent_sku, ratio) > 0;
        } catch (Exception e) {
            logger.error("Error adding BOM item to child {}: parent: {} q: {} e: {}", child_sku, parent_sku, ratio, e);
            return false;
        }
    }

    public int removeBom(String child_sku, String parent_sku){
        //logger.info("Removing BOM item to child {}: {}", child_sku, parent_sku);

        try{
            String sql = "DELETE FROM bom WHERE child_sku = ? AND parent_sku = ?";
            return db.update(sql, child_sku, parent_sku);
        } catch (Exception e) {
            logger.error("Error removing BOM item to child {}: {} e: {}", child_sku, parent_sku, e);
            return -1;
        }
    }

    private static final RowMapper<Bom> BOM_ROW_MAPPER = (rs, rowNum) -> {
        Bom bom = new Bom();
        bom.setRatio(rs.getDouble("ratio"));
        bom.setChild_sku(rs.getString("child_sku"));
        bom.setParent_sku(rs.getString("parent_sku"));
        return bom;
    };

}
