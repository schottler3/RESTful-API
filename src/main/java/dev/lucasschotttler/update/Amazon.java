package dev.lucasschotttler.update;

import org.springframework.stereotype.Service;
import java.util.HashMap;

@Service
public class Amazon {
    
    public static HashMap<String, Double> getPrices(double basePrice) {

        HashMap<String, Double> amazonPrices = new HashMap<>();

        double minimum_price = 999.99;
        double middle_price = 999.99;
        double maximum_price = 999.99;

        if (basePrice < 10) {
            minimum_price = basePrice * 2.062;
            middle_price = minimum_price + 6.5;
            maximum_price = minimum_price * 4.2;
        } else if (basePrice < 25) {
            minimum_price = basePrice * 1.622;
            middle_price = minimum_price + 7.5;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 100) {
            minimum_price = basePrice * 1.342;
            middle_price = minimum_price + 8;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 200) {
            minimum_price = basePrice * 1.347;
            middle_price = minimum_price + 12;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 500) {
            minimum_price = basePrice * 1.272;
            middle_price = minimum_price + 15;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 1000) {
            minimum_price = basePrice * 1.282;
            middle_price = minimum_price + 20;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 5000) {
            minimum_price = basePrice * 1.27;
            middle_price = minimum_price + 40;
            maximum_price = minimum_price * 3;
        } else if (basePrice < 10000) {
            minimum_price = basePrice * 1.222;
            middle_price = minimum_price + 100;
            maximum_price = minimum_price * 3;
        } else {
            minimum_price = basePrice * 1.3;
            middle_price = minimum_price + 200;
            maximum_price = minimum_price * 3;
        }

        // Round prices to 2 decimal places
        minimum_price = Math.round(minimum_price * 100.0) / 100.0;
        middle_price = Math.round(middle_price * 100.0) / 100.0;
        maximum_price = Math.round(maximum_price * 100.0) / 100.0;

        // Ensure prices are not less than 2.5
        if (minimum_price < 2.5) minimum_price = 2.5;
        if (middle_price < 2.5) middle_price = 2.5;
        if (maximum_price < 2.5) maximum_price = 2.5;

        // Add prices to the HashMap
        amazonPrices.put("minimum_price", minimum_price);
        amazonPrices.put("middle_price", middle_price);
        amazonPrices.put("maximum_price", maximum_price);

        return amazonPrices;
    }
}
