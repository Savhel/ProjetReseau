package yowyob.resource.management.controllers.resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.time.LocalDateTime;

import yowyob.resource.management.actions.Action;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.services.product.ProductEntityManager;
import yowyob.resource.management.actions.resource.operations.ResourceCreationAction;
import yowyob.resource.management.actions.resource.operations.ResourceDeletionAction;
import yowyob.resource.management.actions.resource.operations.ResourceReadingAction;
import yowyob.resource.management.actions.resource.operations.ResourceUpdateAction;

@RestController
@RequestMapping("/resource")
public class ResourceController {
    private final ProductEntityManager productEntityManager;

    @Autowired
    public ResourceController(ProductEntityManager productEntityManager) {
        this.productEntityManager = productEntityManager;
    }

    @PostMapping
    public ResponseEntity<?> createResource(@RequestBody Resource resource) {
        productEntityManager.executeAction(new ResourceCreationAction(resource));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResourceById(@PathVariable UUID id) {
        Action action = new ResourceReadingAction(id);
        return ResponseEntity.ok(productEntityManager.executeAction(action));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResource(@RequestBody Resource resource) {
        ResourceUpdateAction action = new ResourceUpdateAction(resource);
        return ResponseEntity.ok(productEntityManager.executeAction(action));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable UUID id) {
        productEntityManager.executeAction(new ResourceDeletionAction(id));
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/schedule/create")
    public ResponseEntity<?> scheduleCreate(@RequestBody Resource resource, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ResourceCreationAction(resource));
        return ResponseEntity.accepted().build();
    }

    /* Just for testing, must delete later */
    @PostMapping("/schedule/read/{id}")
    public ResponseEntity<?> scheduleCreate(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ResourceReadingAction(id));
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/schedule/update")
    public ResponseEntity<?> scheduleUpdate(@RequestBody Resource resource, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ResourceUpdateAction(resource));
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/schedule/delete/{id}")
    public ResponseEntity<?> scheduleDelete(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        this.productEntityManager.scheduleEvent(startDateTime, new ResourceDeletionAction(id));
        return ResponseEntity.accepted().build();
    }
}
