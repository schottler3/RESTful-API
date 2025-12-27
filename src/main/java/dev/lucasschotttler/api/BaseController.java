package dev.lucasschotttler.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasschotttler.database.postgreSQL;

@RestController
public class BaseController {

    private final postgreSQL db;

    public BaseController(postgreSQL db) {
        this.db = db;
    }

    @GetMapping("/")
    public ResponseEntity<String> endpoints() {
        String endpointInfo = """
            {"endpoints": 
                [
                    "superior",
                    "earthpol"
                ]
            }
        """;
        return ResponseEntity.status(HttpStatus.OK).body(endpointInfo);
    }
}

@RestController
@RequestMapping("/superior")
class SuperiorController {

    private final postgreSQL db;

    public SuperiorController(postgreSQL db) {
        this.db = db;
    }

    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.status(HttpStatus.OK).body(db.testConnection());
    }

    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> tools() {
        return ResponseEntity.status(HttpStatus.OK).body(db.getData("superior"));
    }
}