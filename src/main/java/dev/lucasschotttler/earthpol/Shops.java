package dev.lucasschotttler.earthpol;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class Shops {
    
    private final RestTemplate restTemplate;
    
    public Shops(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public Shop[] getShops() {
        String url = "https://api.earthpol.com/astra/shops";
        return restTemplate.getForObject(url, Shop[].class);
    }
}