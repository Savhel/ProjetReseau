package yowyob.resource.management.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheMetrics {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheMetrics.class);
    
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer cacheAccessTimer;

    @Autowired
    public RedisCacheMetrics(MeterRegistry meterRegistry, 
                           CacheManager cacheManager,
                           ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        
        // Initialisation des métriques
        this.cacheHitCounter = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .tag("cache.name", "all")
                .register(meterRegistry);
                
        this.cacheMissCounter = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .tag("cache.name", "all")
                .register(meterRegistry);
                
        this.cacheAccessTimer = Timer.builder("cache.access.time")
                .description("Cache access time")
                .register(meterRegistry);
    }

    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hits")
                .description("Number of cache hits")
                .tag("cache.name", cacheName)
                .register(meterRegistry)
                .increment();
        logger.debug("Cache hit recorded for cache: {}", cacheName);
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.misses")
                .description("Number of cache misses")
                .tag("cache.name", cacheName)
                .register(meterRegistry)
                .increment();
        logger.debug("Cache miss recorded for cache: {}", cacheName);
    }

    public Timer.Sample startCacheAccessTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCacheAccessTime(Timer.Sample sample, String cacheName, String operation) {
        sample.stop(Timer.builder("cache.access.time")
                .tag("cache.name", cacheName)
                .tag("operation", operation)
                .register(meterRegistry));
    }

    @Scheduled(fixedRate = 60000) // Toutes les minutes
    public Mono<Void> reportCacheStatistics() {
        try {
            logger.info("=== Redis Cache Statistics ===");

            // Statistiques par cache (synchrone)
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    logger.info("Cache '{}' - Active", cacheName);
                }
            });

            // Statistiques Redis générales (réactive)
            reactiveRedisTemplate.execute(connection -> connection.serverCommands().dbSize())
                    .next()  // prend le premier élément en Mono
                    .doOnSuccess(dbSize -> {
                        logger.info("Redis DB Size: {} keys", dbSize);
                        meterRegistry.gauge("redis.db.size", dbSize);
                    })
                    .doOnError(error -> logger.warn("Could not retrieve Redis statistics: {}", error.getMessage()))
                    .onErrorReturn(0L)
                    .subscribe();


        } catch (Exception e) {
            logger.error("Error reporting cache statistics", e);
            return Mono.empty();
        }
        return Mono.empty();
    }

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void reportDetailedMetrics() {
        try {
            double hitRate = calculateHitRate();
            logger.info("Cache Hit Rate: {:.2f}%", hitRate * 100);
            
            // Enregistrer la métrique du taux de hit
            meterRegistry.gauge("cache.hit.rate", hitRate);
            
        } catch (Exception e) {
            logger.error("Error calculating detailed metrics", e);
        }
    }

    private double calculateHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return hits / total;
    }

    public void clearMetrics() {
        logger.info("Clearing cache metrics");
        // Note: Micrometer counters cannot be reset, but we can log this action
    }
}