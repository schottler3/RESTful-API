SELECT square_variation_id from superior WHERE square_variation_id IS NOT NULL AND custom_quantity > 0;

SELECT sku, custom_quantity, square_variation_id FROM superior WHERE custom_quantity > 0;

SELECT last_ebay FROM superior ORDER BY last_ebay DESC;

SELECT sku, last_ebay FROM superior WHERE last_ebay IS NULL;

SELECT sku, last_ebay, last_amazon FROM superior WHERE last_ebay IS NULL and last_amazon is NULL;

CREATE TABLE orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    VARCHAR(128)             NOT NULL,
    marketplace VARCHAR(64)              NOT NULL,
    status      VARCHAR(64),
    items       JSONB                    NOT NULL,
    time_placed TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT uq_marketplace_order UNIQUE (marketplace, order_id)
);

ALTER TABLE bom DROP COLUMN quantity;
ALTER TABLE bom ADD COLUMN ratio NUMERIC;

SELECT * FROM orders;

SELECT * FROM bom;

ALTER TABLE superior ADD COLUMN square_quantity INT;