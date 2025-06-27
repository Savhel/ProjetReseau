package yowyob.resource.management.controllers;

import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/status")
public class ApiStatus {

    @GetMapping
    public Mono<String> getServiceById() {
        return Mono.just("resource/service api is up xD");
    }
}