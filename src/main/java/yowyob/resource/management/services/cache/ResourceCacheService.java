package yowyob.resource.management.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.resource.management.models.resource.Resource;
import yowyob.resource.management.repositories.resource.ResourceRepository;

import java.time.Duration;
import java.util.UUID;

@Service
public class ResourceCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheService.class);
    private final ResourceRepository resourceRepository;
    private final ReactiveCacheService reactiveCacheService;
    
    private static final Duration RESOURCE_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration ALL_RESOURCES_CACHE_TTL = Duration.ofMinutes(5);
    private static final String RESOURCE_CACHE_PREFIX = "resource";
    private static final String ALL_RESOURCES_KEY = "resources:all";

    @Autowired
    public ResourceCacheService(ResourceRepository resourceRepository, ReactiveCacheService reactiveCacheService) {
        this.resourceRepository = resourceRepository;
        this.reactiveCacheService = reactiveCacheService;
    }

    public Mono<Resource> findById(UUID id) {
        String cacheKey = reactiveCacheService.generateKey(RESOURCE_CACHE_PREFIX, id);
        logger.debug("Fetching resource from cache or database with id: {}", id);
        
        return reactiveCacheService.getOrCompute(
            cacheKey,
            resourceRepository.findById(id),
            RESOURCE_CACHE_TTL,
            Resource.class
        );
    }

    public Flux<Resource> findAll() {
        logger.debug("Fetching all resources from cache or database");
        
        return reactiveCacheService.getOrComputeList(
            ALL_RESOURCES_KEY,
            resourceRepository.findAll(),
            ALL_RESOURCES_CACHE_TTL,
            Resource.class
        );
    }

    public Mono<Resource> save(Resource resource) {
        logger.debug("Saving resource to database and updating cache: {}", resource.getId());
        
        return resourceRepository.save(resource)
                .flatMap(savedResource -> {
                    String cacheKey = reactiveCacheService.generateKey(RESOURCE_CACHE_PREFIX, savedResource.getId());
                    
                    // Mettre à jour le cache individuel et invalider le cache "all"
                    return reactiveCacheService.put(cacheKey, savedResource, RESOURCE_CACHE_TTL)
                            .then(reactiveCacheService.evict(ALL_RESOURCES_KEY))
                            .thenReturn(savedResource);
                });
    }

    public Mono<Void> deleteById(UUID id) {
        logger.debug("Deleting resource from database and cache: {}", id);
        
        String cacheKey = reactiveCacheService.generateKey(RESOURCE_CACHE_PREFIX, id);
        
        return resourceRepository.deleteById(id)
                .then(reactiveCacheService.evict(cacheKey))
                .then(reactiveCacheService.evict(ALL_RESOURCES_KEY)).then();
    }

    public Mono<Void> clearCache() {
        logger.info("Clearing all resources cache");
        
        return reactiveCacheService.evictPattern(RESOURCE_CACHE_PREFIX + ":*")
                .then(reactiveCacheService.evict(ALL_RESOURCES_KEY))
                .then();
    }

    public Mono<Boolean> existsById(UUID id) {
        String cacheKey = reactiveCacheService.generateKey(RESOURCE_CACHE_PREFIX, id);
        
        // Vérifier d'abord dans le cache, puis en base si nécessaire
        return reactiveCacheService.exists(cacheKey)
                .flatMap(existsInCache -> {
                    if (existsInCache) {
                        return Mono.just(true);
                    }
                    return resourceRepository.existsById(id);
                });
    }
}