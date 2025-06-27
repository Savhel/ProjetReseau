package yowyob.resource.management.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class ReactiveCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveCacheService.class);
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Autowired
    public ReactiveCacheService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Récupère une valeur du cache ou l'exécute si elle n'existe pas
     */
    public <T> Mono<T> getOrCompute(String key, Mono<T> computation, Duration ttl, Class<T> type) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .cast(type)
                .doOnNext(cached -> logger.debug("Cache hit for key: {}", key))
                .switchIfEmpty(
                    computation
                        .doOnNext(computed -> logger.debug("Cache miss for key: {}, computing value", key))
                        .flatMap(value -> 
                            reactiveRedisTemplate.opsForValue()
                                .set(key, value, ttl)
                                .thenReturn(value)
                        )
                )
                .onErrorResume(error -> {
                    logger.warn("Cache error for key: {}, falling back to computation: {}", key, error.getMessage());
                    return computation;
                });
    }

    /**
     * Récupère une liste du cache ou l'exécute si elle n'existe pas
     */
    public <T> Flux<T> getOrComputeList(String key, Flux<T> computation, Duration ttl, Class<T> type) {
        return reactiveRedisTemplate.opsForList()
                .range(key, 0, -1)
                .cast(type)
                .switchIfEmpty(
                    computation
                        .collectList()
                        .doOnNext(list -> logger.debug("Cache miss for list key: {}, computing {} items", key, list.size()))
                        .flatMapMany(list -> 
                            reactiveRedisTemplate.opsForList()
                                .rightPushAll(key, list.toArray())
                                .then(reactiveRedisTemplate.expire(key, ttl))
                                .thenMany(Flux.fromIterable(list))
                        )
                )
                .doOnNext(item -> logger.debug("Retrieved item from cache for key: {}", key))
                .onErrorResume(error -> {
                    logger.warn("Cache error for list key: {}, falling back to computation: {}", key, error.getMessage());
                    return computation;
                });
    }

    /**
     * Met à jour une valeur dans le cache
     */
    public <T> Mono<T> put(String key, T value, Duration ttl) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(success -> logger.debug("Updated cache for key: {}", key))
                .thenReturn(value)
                .onErrorResume(error -> {
                    logger.warn("Failed to update cache for key: {}: {}", key, error.getMessage());
                    return Mono.just(value);
                });
    }

    /**
     * Supprime une entrée du cache
     */
    public Mono<Boolean> evict(String key) {
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnNext(deleted -> {
                    if (deleted) {
                        logger.debug("Evicted cache entry for key: {}", key);
                    } else {
                        logger.debug("No cache entry found to evict for key: {}", key);
                    }
                })
                .onErrorResume(error -> {
                    logger.warn("Failed to evict cache for key: {}: {}", key, error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Supprime toutes les entrées correspondant à un pattern
     */
    public Mono<Long> evictPattern(String pattern) {
        return reactiveRedisTemplate.keys(pattern)
                .flatMap(reactiveRedisTemplate::delete)
                .reduce(0L, Long::sum)
                .doOnNext(count -> logger.debug("Evicted {} cache entries for pattern: {}", count, pattern))
                .onErrorResume(error -> {
                    logger.warn("Failed to evict cache entries for pattern: {}: {}", pattern, error.getMessage());
                    return Mono.just(0L);
                });
    }

    /**
     * Vérifie si une clé existe dans le cache
     */
    public Mono<Boolean> exists(String key) {
        return reactiveRedisTemplate.hasKey(key)
                .onErrorResume(error -> {
                    logger.warn("Failed to check cache existence for key: {}: {}", key, error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Génère une clé de cache standardisée
     */
    public String generateKey(String prefix, Object... parts) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object part : parts) {
            keyBuilder.append(":").append(part);
        }
        return keyBuilder.toString();
    }
}