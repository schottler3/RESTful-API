CREATE TABLE IF NOT EXISTS superior (
    lakesid INT PRIMARY KEY,
    width FLOAT,
    length FLOAT,
    height FLOAT,
    weight FLOAT,
    type VARCHAR(128),
    mpn VARCHAR(128),
    title VARCHAR(256),
    description TEXT,
    upc VARCHAR(128),
    brand VARCHAR(128),
    quantity INT NOT NULL,
    
    sku VARCHAR(255) NOT NULL,
    name TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- docker exec -i postgres-dev psql -U schottler3 -d superior < superior_schema.sql

/*
    inventory_item_data = {
        "product": {
            "aspects": {
                "width": [str(width)],
                "length": [str(length)],
                "height": [str(height)],
                "weight": [str(weight)],
                "Brand": [brand],
                "Part Type": [type],
                "Type": [type],
            },
            "brand": brand,
            "mpn": mpn,
            "title": title,
            "description": description,
            "upc": [UPC],
            "imageUrls": [imageURL] if imageURL else []
        },
        "condition": "NEW",
        "packageWeightAndSize": {
            "dimensions": {
                "height": packageHeight,
                "length": packageLength,
                "width": packageWidth,
                "unit": "INCH"
            },
            "weight": {
                "value": packageWeight,
                "unit": "POUND"
            }
        },
        "availability": {
            "shipToLocationAvailability": {
                "fulfillmentTime": {
                    "unit": "BUSINESS_DAY",
                    "value": fulfillment
                },
                "quantity": quantity,
                "merchantLocationKey": MERCHANT_LOCATION_KEY
            }
        }
    }
*/