package dev.lucasschottler.marketplaces.ingresTypes;

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
