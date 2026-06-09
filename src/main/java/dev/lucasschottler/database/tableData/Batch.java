package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class Batch {

    public String batch_id;
    public Integer num_success;
    public Integer num_failure;
    public Timestamp timestamp;

}
