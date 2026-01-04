package dev.lucasschotttler.database;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.lucasschotttler.earthpol.Location;
import dev.lucasschotttler.earthpol.Shop;
import dev.lucasschotttler.earthpol.Shops;
import dev.lucasschotttler.earthpol.Location.LocationRequest;
import dev.lucasschotttler.earthpol.Location.LocationResponse;

@Repository
public class postgreSQL {
    
    private final JdbcTemplate jdbcTemplate;
    private final Location location;
    
    public postgreSQL(JdbcTemplate jdbcTemplate, Location location) {
        this.jdbcTemplate = jdbcTemplate;
        this.location = location;
    }
    
    public String testConnection() {
        return jdbcTemplate.queryForObject("SELECT version()", String.class);
    }
    
    public List<String> getShopsByQuery(String query) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM shops WHERE name LIKE ?", 
            String.class,
            "%" + query + "%"
        );
    }

    public String UpdateShops(Shops shops) {
        Shop[] allShops = shops.getShops();

        for (Shop shop : allShops) {
            String sql = "INSERT INTO shops (id, owner, item, price, type, space, stock, world, x, y, z, nation, town, wilderness) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            Location ShopLocation = shop.getLocation();
            
            LocationResponse response = location.getLocationObject(ShopLocation.getX(), ShopLocation.getZ());

            jdbcTemplate.update(sql, 
                shop.getId(),
                shop.getOwner(),
                shop.getItem(),
                shop.getPrice(),
                shop.getType(),
                shop.getSpace(),
                shop.getStock(),
                ShopLocation.getWorld(),
                ShopLocation.getX(),
                ShopLocation.getY(),
                ShopLocation.getZ(),
                response.nation != null ? response.nation.uuid : null,
                response.town != null ? response.town.uuid : null,
                response.isWilderness
            );
        }
        
        return "Success";
    }
}