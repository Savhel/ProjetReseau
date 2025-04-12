package yowyob.resource.management.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/status")
public class ApiStatus {

    @GetMapping
    public ResponseEntity<String> getServiceById() {
        return ResponseEntity.ok("resource/service api is up xD");
    }
}