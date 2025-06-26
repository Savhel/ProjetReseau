package yowyob.resource.management.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import yowyob.resource.management.models.service.Services;
import yowyob.resource.management.repositories.service.ServiceRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServiceCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCacheService.class);
    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceCacheService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Cacheable(value = "services", key = "#id")
    public Optional<Services> findById(UUID id) {
        logger.debug("Fetching service from database with id: {}", id);
        return serviceRepository.findById(id);
    }

    @Cacheable(value = "services", key = "'all'")
    public List<Services> findAll() {
        logger.debug("Fetching all services from database");
        return serviceRepository.findAll();
    }

    @CachePut(value = "services", key = "#service.id")
    @CacheEvict(value = "services", key = "'all'")
    public Services save(Services service) {
        logger.debug("Saving service to database and updating cache: {}", service.getId());
        return serviceRepository.save(service);
    }

    @CacheEvict(value = "services", key = "#id")
    public void deleteById(UUID id) {
        logger.debug("Deleting service from database and cache: {}", id);
        serviceRepository.deleteById(id);
    }

    @CacheEvict(value = "services", allEntries = true)
    public void clearCache() {
        logger.info("Clearing all services cache");
    }

    public boolean existsById(UUID id) {
        // Pour les vérifications d'existence, on peut d'abord vérifier le cache
        Optional<Services> cached = findById(id);
        if (cached.isPresent()) {
            return true;
        }
        // Si pas en cache, vérifier en base
        return serviceRepository.existsById(id);
    }
}