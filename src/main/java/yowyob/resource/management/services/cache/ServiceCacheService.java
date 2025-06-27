package yowyob.resource.management.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.repositories.service.ServiceRepository;

import java.time.Duration;
import java.util.UUID;

@Service
public class ServiceCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCacheService.class);
    private final ServiceRepository serviceRepository;
    private final ReactiveCacheService reactiveCacheService;
    
    private static final Duration SERVICE_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration ALL_SERVICES_CACHE_TTL = Duration.ofMinutes(5);
    private static final String SERVICE_CACHE_PREFIX = "service";
    private static final String ALL_SERVICES_KEY = "services:all";

    @Autowired
    public ServiceCacheService(ServiceRepository serviceRepository, ReactiveCacheService reactiveCacheService) {
        this.serviceRepository = serviceRepository;
        this.reactiveCacheService = reactiveCacheService;
    }

    public Mono<Services> findById(UUID id) {
        String cacheKey = reactiveCacheService.generateKey(SERVICE_CACHE_PREFIX, id);
        logger.debug("Fetching service from cache or database with id: {}", id);
        
        return reactiveCacheService.getOrCompute(
            cacheKey,
            serviceRepository.findById(id),
            SERVICE_CACHE_TTL,
            Services.class
        );
    }

    public Flux<Services> findAll() {
        logger.debug("Fetching all services from cache or database");
        
        return reactiveCacheService.getOrComputeList(
            ALL_SERVICES_KEY,
            serviceRepository.findAll(),
            ALL_SERVICES_CACHE_TTL,
            Services.class
        );
    }

    public Mono<Services> save(Services service) {
        logger.debug("Saving service to database and updating cache: {}", service.getId());
        
        return serviceRepository.save(service)
                .flatMap(savedService -> {
                    String cacheKey = reactiveCacheService.generateKey(SERVICE_CACHE_PREFIX, savedService.getId());
                    
                    // Mettre à jour le cache individuel et invalider le cache "all"
                    return reactiveCacheService.put(cacheKey, savedService, SERVICE_CACHE_TTL)
                            .then(reactiveCacheService.evict(ALL_SERVICES_KEY))
                            .thenReturn(savedService);
                });
    }

    public Mono<Void> deleteById(UUID id) {
        logger.debug("Deleting service from database and cache: {}", id);
        
        String cacheKey = reactiveCacheService.generateKey(SERVICE_CACHE_PREFIX, id);
        
        return serviceRepository.deleteById(id)
                .then(reactiveCacheService.evict(cacheKey))
                .then(reactiveCacheService.evict(ALL_SERVICES_KEY))
                .then();
    }

    public Mono<Void> clearCache() {
        logger.info("Clearing all services cache");
        
        return reactiveCacheService.evictPattern(SERVICE_CACHE_PREFIX + ":*")
                .then(reactiveCacheService.evict(ALL_SERVICES_KEY))
                .then();
    }

    public Mono<Boolean> existsById(UUID id) {
        String cacheKey = reactiveCacheService.generateKey(SERVICE_CACHE_PREFIX, id);
        
        // Vérifier d'abord dans le cache, puis en base si nécessaire
        return reactiveCacheService.exists(cacheKey)
                .flatMap(existsInCache -> {
                    if (existsInCache) {
                        return Mono.just(true);
                    }
                    return serviceRepository.existsById(id);
                });
    }
}