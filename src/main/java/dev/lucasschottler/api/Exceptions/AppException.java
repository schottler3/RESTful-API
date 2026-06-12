package dev.lucasschottler.api.Exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppException extends RuntimeException {
    private final List<String> context = new ArrayList<>();
    private String sku;
    private UUID batchId;

    public AppException(String message, String sku, UUID batchId) {
        context.add(message);
        this.sku = sku;
        this.batchId = batchId;
    }

    public AppException addContext(String message, Class<?> clazz){
        this.context.add(clazz.getSimpleName() + ": " + message);
        return this;
    }

    public String getFullContext() {
        return String.join(" ->\n", context);
    }

    public String getSku() {
        return sku;
    }

    public UUID getBatchId() {
        return batchId;
    }
}
