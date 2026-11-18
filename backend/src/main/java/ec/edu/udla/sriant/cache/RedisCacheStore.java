package ec.edu.udla.sriant.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Caché cloud sobre Redis (Upstash / Redis Cloud / Azure Cache for Redis).
 * Si Redis no responde, la caché se degrada a "miss" y la API sigue funcionando contra el servicio externo.
 */
@Component
public class RedisCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheStore.class);
    private final StringRedisTemplate redis;

    public RedisCacheStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key));
        } catch (RuntimeException ex) {
            log.warn("Redis no disponible al leer '{}': {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (RuntimeException ex) {
            log.warn("Redis no disponible al escribir '{}': {}", key, ex.getMessage());
        }
    }
}
