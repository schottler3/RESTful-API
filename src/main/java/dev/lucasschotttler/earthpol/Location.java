package dev.lucasschotttler.earthpol;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class Location {

    private RestTemplate restTemplate = new RestTemplate();

    private String world;
    private double x;
    private double y;
    private double z;
    
    public Location(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public LocationResponse getLocationObject(double x, double z) {
        String url = "https://api.earthpol.com/astra/location";
        
        LocationRequest request = new LocationRequest(x, z);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<LocationRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(url, entity, LocationResponse.class);
    }
    
    // Request structure
    public static class LocationRequest {
        public double[][] query;
        
        public LocationRequest(double x, double z) {
            this.query = new double[][]{{x, z}};
        }
    }
    
    // Response structure
    public static class LocationResponse {
        public LocationCoords location;
        public boolean isWilderness;
        public Town town;
        public Nation nation;
        
        public static class LocationCoords {
            public double x;
            public double z;
        }
        
        public static class Town {
            public String name;
            public String uuid;
        }
        
        public static class Nation {
            public String name;
            public String uuid;
        }
    }
}