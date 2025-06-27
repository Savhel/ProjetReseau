package yowyob.resource.management.controllers.service;

import java.util.UUID;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Autowired;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.actions.service.ServiceAction;
import yowyob.resource.management.services.product.ProductEntityManager;
import yowyob.resource.management.actions.service.operations.ServiceUpdateAction;
import yowyob.resource.management.actions.service.operations.ServiceReadingAction;
import yowyob.resource.management.actions.service.operations.ServiceDeletionAction;
import yowyob.resource.management.actions.service.operations.ServiceCreationAction;


@RestController
@RequestMapping("/service")
public class ServiceController {

    private final ProductEntityManager productEntityManager;
    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    @Autowired
    public ServiceController(ProductEntityManager productEntityManager) {
        this.productEntityManager = productEntityManager;
    }

    @PostMapping
    public Mono<Services> createService(@RequestBody Services service) {
        return productEntityManager.executeAction(new ServiceCreationAction(service))
                .cast(Services.class);
    }

    @GetMapping("/{id}")
    public Mono<Services> getServiceById(@PathVariable UUID id) {
        Action action = new ServiceReadingAction(id);
        return productEntityManager.executeAction(action)
                .cast(Services.class);
    }

    @PutMapping("/{id}")
    public Mono<Services> updateService(@RequestBody Services service) {
        ServiceUpdateAction action = new ServiceUpdateAction(service);
        return productEntityManager.executeAction(action)
                .cast(Services.class);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteService(@PathVariable UUID id) {
        return productEntityManager.executeAction(new ServiceDeletionAction(id))
                .then();
    }


    @PostMapping("/schedule/create")
    public Mono<Void> scheduleCreate(@RequestBody Services service, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ServiceCreationAction(service));
    }

    /* Just for testing, must delete later */
    @PostMapping("/schedule/read/{id}")
    public Mono<Void> scheduleRead(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ServiceReadingAction(id));
    }

    @PutMapping("/schedule/update")
    public Mono<Void> scheduleUpdate(@RequestBody Services service, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ServiceUpdateAction(service));
    }

    @DeleteMapping("/schedule/delete/{id}")
    public Mono<Void> scheduleDelete(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ServiceDeletionAction(id));
    }
}
