package ec.edu.udla.sriant.cache;

import java.time.Duration;
import java.util.Optional;

/** Abstracción del almacenamiento de caché (Redis en la nube en producción, memoria en pruebas). */
public interface CacheStore {
    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);
}
