package dev.lucasschottler.database.tableData;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.Data;

@Data
public class Batch {

    public UUID batch_id;
    public Integer num_success;
    public Integer num_failure;
    public Timestamp timestamp;

}
