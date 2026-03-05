package dev.lucasschottler.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschottler.database.Databasing;

@RestController
public class BaseController {

    private final Databasing db;

    public BaseController(Databasing db) {
        this.db = db;
    }

    @GetMapping("/")
    public ResponseEntity<String> endpoints() {
        String endpointInfo = """
            {"endpoints": 
                [
                    "superior"
                ]
            }
        """;
        return ResponseEntity.status(HttpStatus.OK).body(endpointInfo);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\"}");
    }

    @GetMapping("/superior/images/{SKU}")
    public ResponseEntity<List<String>> tools(@PathVariable String SKU) {
        return ResponseEntity.status(HttpStatus.OK).body(db.getImages(SKU));
    }
}