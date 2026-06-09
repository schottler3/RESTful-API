package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class Error {

    public String batch_id;
    public String sku;
    public String error_message;
    public Timestamp timestamp;

}
