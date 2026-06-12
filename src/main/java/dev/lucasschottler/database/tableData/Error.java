package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.Data;

@Data
public class Error {

    public UUID batch_id;
    public String sku;
    public String error_message;
    public Timestamp timestamp;

}
