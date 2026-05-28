package dev.lucasschottler.database.tableData;

import lombok.Data;

@Data
public class Bom {
    
    //Ratio of the child.quantity * ratio

    public Double ratio;
    public String child_sku;
    public String parent_sku;

}
