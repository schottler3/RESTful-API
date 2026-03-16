package dev.lucasschottler.api.alternative;

import dev.lucasschottler.database.DatabaseItem;

public class Alternative {

    public DatabaseItem dbItem;
    public boolean is_ebay;
    public boolean is_amazon;

    public Alternative(DatabaseItem dbItem, String alt_sku, boolean is_ebay, boolean is_amazon){

        this.dbItem = dbItem;
        this.dbItem.sku = alt_sku;
        this.is_ebay = is_ebay;
        this.is_amazon = is_amazon;

    }

}
