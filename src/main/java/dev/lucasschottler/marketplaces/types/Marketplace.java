package dev.lucasschottler.marketplaces.types;

public enum Marketplace {
    AMAZON("amazon"),
    EBAY("ebay");

    private final String value;
    

    Marketplace(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
