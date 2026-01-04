package dev.lucasschotttler.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschotttler.database.postgreSQL;
import dev.lucasschotttler.earthpol.Shop;
import dev.lucasschotttler.earthpol.Shops;

@RestController
@RequestMapping("/shops")
public class EarthpolController {

    private final postgreSQL db;
    private final Shops shops;

    public EarthpolController(postgreSQL db, Shops shops) {
        this.db = db;
        this.shops = shops;
    }

    @GetMapping
    public ResponseEntity<Shop[]> getShops() {
        return ResponseEntity.status(HttpStatus.OK).body(shops.getShops());
    }

    @GetMapping("/update")
    public ResponseEntity<String> updateShops() {
        return ResponseEntity.status(HttpStatus.OK).body(db.UpdateShops(shops));
    }
}

@RestController
@RequestMapping("/shops/search")
class DataController {

    private final postgreSQL db;

    public DataController(postgreSQL db) {
        this.db = db;
    }

    @GetMapping("/{query}")
    public ResponseEntity<List<String>> tools(@PathVariable String query) {
        return ResponseEntity.status(HttpStatus.OK).body(db.getShopsByQuery(query));
    }

}