package yowyob.resource.management.controllers.resource;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

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
    public Mono<Resource> createResource(@RequestBody Resource resource) {
        return productEntityManager.executeAction(new ResourceCreationAction(resource))
                .cast(Resource.class);
    }

    @GetMapping("/{id}")
    public Mono<Resource> getResourceById(@PathVariable UUID id) {
        Action action = new ResourceReadingAction(id);
        return productEntityManager.executeAction(action)
                .cast(Resource.class);
    }

    @PutMapping("/{id}")
    public Mono<Resource> updateResource(@RequestBody Resource resource) {
        ResourceUpdateAction action = new ResourceUpdateAction(resource);
        return productEntityManager.executeAction(action)
                .cast(Resource.class);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteResource(@PathVariable UUID id) {
        return productEntityManager.executeAction(new ResourceDeletionAction(id))
                .then();
    }


    @PostMapping("/schedule/create")
    public Mono<Void> scheduleCreate(@RequestBody Resource resource, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ResourceCreationAction(resource));
    }

    /* Just for testing, must delete later */
    @PostMapping("/schedule/read/{id}")
    public Mono<Void> scheduleRead(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ResourceReadingAction(id));
    }

    @PutMapping("/schedule/update")
    public Mono<Void> scheduleUpdate(@RequestBody Resource resource, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ResourceUpdateAction(resource));
    }

    @DeleteMapping("/schedule/delete/{id}")
    public Mono<Void> scheduleDelete(@PathVariable UUID id, @RequestParam LocalDateTime startDateTime) {
        return this.productEntityManager.scheduleEvent(startDateTime, new ResourceDeletionAction(id));
    }
}
