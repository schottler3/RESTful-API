package dev.lucasschotttler.earthpol;

public class Shop {
    
    private int id;
    private String owner;
    private String item;
    private double price;
    private String type;
    private int space;
    private int stock;
    private Location location;
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getSpace() { return space; }
    public void setSpace(int space) { this.space = space; }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
}
