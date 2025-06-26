package yowyob.resource.management.controllers.service;

import java.util.UUID;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

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
    public ResponseEntity<?> createService(@RequestBody Services services) {
        productEntityManager.executeAction(new ServiceCreationAction(services));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable UUID id) {
        ServiceAction action = new ServiceReadingAction(id);
        return ResponseEntity.ok(productEntityManager.executeAction(action));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@RequestBody Services services) {
        ServiceAction action = new ServiceUpdateAction(services);
        return ResponseEntity.ok(productEntityManager.executeAction(action));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable UUID id) {
        productEntityManager.executeAction(new ServiceDeletionAction(id));
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/schedule/create")
    public ResponseEntity<?> scheduleCreate(@RequestBody Services services, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ServiceCreationAction(services));
        return ResponseEntity.accepted().build();
    }

    /* Just for testing, must delete later */
    @PostMapping("/schedule/read/{id}")
    public ResponseEntity<?> scheduleCreate(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ServiceReadingAction(id));
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/schedule/update")
    public ResponseEntity<?> scheduleUpdate(@RequestBody Services services, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ServiceUpdateAction(services));
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/schedule/delete/{id}")
    public ResponseEntity<?> scheduleDelete(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ServiceDeletionAction(id));
        return ResponseEntity.accepted().build();
    }
}
