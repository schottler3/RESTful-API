package dev.lucasschottler.database;

import java.util.List;
import java.util.Set;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.lucasschottler.database.tableData.Order;
import dev.lucasschottler.marketplaces.ingresTypes.AmazonOrder;
import dev.lucasschottler.marketplaces.ingresTypes.EbayOrderConfirmation;
import dev.lucasschottler.marketplaces.ingresTypes.Marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;


@Service
public class Databasing {

    private static final Logger logger = LoggerFactory.getLogger(Databasing.class);   
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> allowedMarketplaces = Set.of("amazon", "ebay");

    private final JdbcTemplate jdbcTemplate;
      
    public Databasing(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean updateLastSuccess(String marketplace, String sku){

        if(!allowedMarketplaces.contains(marketplace)){
            return false;
        }

        String sql = "UPDATE superior SET last_" + marketplace + " = NOW() WHERE sku = ?;";
        return jdbcTemplate.update(sql, sku) > 0;
    }

    public Integer[] getAllLakesIdsInAsc(){
        String sql = "SELECT lakesid FROM superior ORDER BY lakesid ASC";

        List<Integer> lakesIds = jdbcTemplate.queryForList(sql, Integer.class);
        return lakesIds.toArray(new Integer[0]);
    }

    public String getSkuFromEbayListingId(String ebayListingId){
        String sql = "SELECT sku FROM superior WHERE ebay_listing_id = ? LIMIT 1";

        try {
            return jdbcTemplate.queryForObject(sql, String.class, ebayListingId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String addEbayOrder(EbayOrderConfirmation orderConfirmation) {
        EbayOrderConfirmation.Order order = orderConfirmation.getNotification().getData().getOrder();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = mapper.createArrayNode();

        for (EbayOrderConfirmation.OrderLineItem orderItem : order.getOrderLineItems()) {
            String sku = this.getSkuFromEbayListingId(orderItem.getListingId());

            ObjectNode item = mapper.createObjectNode();
            item.put("sku", sku);
            item.put("quantity", orderItem.getQuantity());

            items.add(item);
        }

        try {
            String itemsJson = mapper.writeValueAsString(items);

            Instant timePlaced = Instant.parse(orderConfirmation.getNotification().getPublishDate());

            String sql = """
                    INSERT INTO orders (order_id, marketplace, status, items, created_at)
                    VALUES (?, ?, ?, ?::jsonb, ?)
                    """;

            jdbcTemplate.update(sql,
                order.getOrderId(),
                "ebay",
                "UNSHIPPED",
                itemsJson,
                Timestamp.from(timePlaced));

            return order.getOrderId();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order items", e);
        }
    }

    public String addAmazonOrder(AmazonOrder amazonOrder) {

        String orderId = amazonOrder.getOrderId();
        String orderStatus = amazonOrder.getFulfillment().getFulfillmentStatus();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = mapper.createArrayNode();

        if(getOrder(orderId) != null){
            if(orderStatus.equals("ACCOUNTED")){
                return orderId;
            }
            updateOrderStatus(orderId, orderStatus);
            return orderId;
        }

        for (AmazonOrder.OrderItem orderItem : amazonOrder.getOrderItems()) {
            String sku = orderItem.getProduct().getSellerSku();
            int quantity = orderItem.getQuantityOrdered();

            ObjectNode item = mapper.createObjectNode();
            item.put("sku", sku);
            item.put("quantity", quantity);

            items.add(item);
        }

        try {
            String itemsJson = mapper.writeValueAsString(items);

            Instant timePlaced = Instant.parse(amazonOrder.getCreatedTime());

            String sql = """
                    INSERT INTO orders (order_id, marketplace, status, items, created_at)
                    VALUES (?, ?, ?, ?::jsonb, ?)
                    """;

            jdbcTemplate.update(sql,
                orderId,
                "amazon",
                orderStatus,
                itemsJson,
                Timestamp.from(timePlaced));

            return orderId;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order items", e);
        }
    }

    public Order getOrder(String orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ? LIMIT 1";

        RowMapper<Order> rowMapper = (rs, rowNum) -> {
        Order order = new Order();
        order.setOrderId(rs.getString("order_id"));
        order.setMarketplace(Marketplace.valueOf(rs.getString("marketplace").toUpperCase()));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));

        try {
            List<Order.Item> items = mapper.readValue(
                rs.getString("items"),
                new TypeReference<List<Order.Item>>() {}
            );
            order.setItems(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize order items", e);
        }

        return order;
    };

        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, orderId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String updateOrderStatus(String orderId, String status){
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        jdbcTemplate.update(sql, status, orderId);
        return orderId;
    }
}