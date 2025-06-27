package yowyob.resource.management.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.repositories.resource.ResourceRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResourceCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheService.class);
    private final ResourceRepository resourceRepository;

    @Autowired
    public ResourceCacheService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Cacheable(value = "resources", key = "#id")
    public Mono<Resource> findById(UUID id) {
        logger.debug("Fetching resource from database with id: {}", id);
        return resourceRepository.findById(id);
    }

    @Cacheable(value = "resources", key = "'all'")
    public Flux<Resource> findAll() {
        logger.debug("Fetching all resources from database");
        return resourceRepository.findAll();
    }

    @CachePut(value = "resources", key = "#resource.id")
    @CacheEvict(value = "resources", key = "'all'")
    public Mono<Resource> save(Resource resource) {
        logger.debug("Saving resource to database and updating cache: {}", resource.getId());
        return resourceRepository.save(resource);
    }

    @CacheEvict(value = "services", key = "#id")
    public Mono<Void> deleteById(UUID id) {
        logger.debug("Deleting resource from database and cache: {}", id);
        resourceRepository.deleteById(id);
        return Mono.empty();
    }

    @CacheEvict(value = "resources", allEntries = true)
    public void clearCache() {
        logger.info("Clearing all resources cache");
    }

    public Mono<Boolean>existsById(UUID id) {
        // Pour les vérifications d'existence, on peut d'abord vérifier le cache
        return findById(id)
                .map(resource -> true)            // Si trouvé dans le cache ou en DB
                .switchIfEmpty(
                        resourceRepository.existsById(id) // Sinon, vérifie en base
                );
    }
}