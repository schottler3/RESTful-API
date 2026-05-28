package dev.lucasschottler.marketplaces.ingresTypes;

import lombok.Data;
import java.util.List;

@Data
public class AmazonOrdersResponse {
    private List<AmazonOrder> orders;
    private String createdBefore;
}
