package dev.lucasschottler.marketplaces.types;

import lombok.Data;
import java.util.List;

@Data
public class AmazonOrdersResponse {
    private List<AmazonOrder> orders;
    private String createdBefore;
}
