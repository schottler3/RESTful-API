package dev.lucasschotttler.api;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dev.lucasschotttler.database.Databasing;
import dev.lucasschotttler.lakes.Lakes;
import dev.lucasschotttler.lakes.LakesItem;
import dev.lucasschotttler.database.DatabaseItem;

@Service
public class Actions {
    
    private final Databasing db;
    private static final Logger logger = LoggerFactory.getLogger(Actions.class);

    public Actions(Databasing db, Lakes lakes){
        this.db = db;
    }

    public void updateInventory(){
        List<Map<String, Object>> data = db.queryDatabase("", 10000);

        data.forEach(item -> {
            DatabaseItem dbItem = new DatabaseItem(item);

            LakesItem lakesItem = Lakes.getLakesItem(dbItem.lakesid);
            
            dbItem.updateItem(lakesItem, db);


            
        });
    }

}
