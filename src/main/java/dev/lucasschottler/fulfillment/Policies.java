package dev.lucasschottler.fulfillment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Policies {

    public enum FulfillmentPolicy {
        DROP_UPS_5D                  ("164478546024"),  // DROP - UPS - 5 Day Handling (Overdimension)
        SPEEDEE_2D                   ("211235510024"),  // SpeeDee Only - 2 Day Handling
        STOCK_USPS_GROUND_0D         ("230760359024"),  // STOCK - USPS Ground Advantage - 0 Day Handling
        DROP_USPS_GROUND_5D          ("232289075024"),  // DROP - USPS Ground Advantage - 5 Day Handling
        STOCK_FEDEX_GROUND_0D        ("240860849024"),  // STOCK - FedEx Ground Economy - 0 Day Handling
        DROP_FEDEX_GROUND_5D         ("241033278024"),  // DROP - FedEx Ground Economy - 5 Day Handling
        STOCK_FREIGHT_BULKY_0D       ("245185146024"),  // STOCK - FREIGHT - BULKY - 0 DAY HANDLING
        DEEP_BACKORDER_USPS_40D      ("252999177024"),  // DEEP BACKORDER - Encompass - USPS Ground - 40 Day Handling
        DROP_USPS_GROUND_5D_COPY2    ("253764683024"),  // DROP - USPS Ground Advantage - 5 Day Handling Copy (2)
        DROP_FEDEX_GROUND_5D_COPY    ("253764693024");  // DROP - FedEx Ground Economy - 5 Day Handling Copy

        private final String id;

        FulfillmentPolicy(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public FulfillmentPolicy getFulfillmentPolicy(int fulfillmentTime, double weight, double length, double width, double height) {
        if (fulfillmentTime <= 1) {
            if (weight < 1) {
                return FulfillmentPolicy.STOCK_USPS_GROUND_0D;
            } else if (weight <= 50) {
                return FulfillmentPolicy.STOCK_FEDEX_GROUND_0D;
            }
        } else if (fulfillmentTime <= 5) {
            if (weight < 1) {
                return FulfillmentPolicy.DROP_USPS_GROUND_5D;
            } else if (weight <= 50) {
                List<Double> dimensions = new ArrayList<>(Arrays.asList(length, width, height)); // fix here
                double L = Collections.max(dimensions);
                dimensions.remove(dimensions.indexOf(L));
                double total = L + (2 * (dimensions.get(0) + dimensions.get(1)));

                if (total <= 130) {
                    return FulfillmentPolicy.DROP_FEDEX_GROUND_5D;
                } else {
                    return FulfillmentPolicy.DROP_UPS_5D;
                }
            }
        } else if (fulfillmentTime >= 6 && weight < 50) {
            return FulfillmentPolicy.DEEP_BACKORDER_USPS_40D;
        }

        return FulfillmentPolicy.STOCK_FREIGHT_BULKY_0D;
    }

}