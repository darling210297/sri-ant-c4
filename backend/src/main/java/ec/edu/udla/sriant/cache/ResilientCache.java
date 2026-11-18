package ec.edu.udla.sriant.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Patrón Cache-Aside con respaldo "stale-if-error":
 * <ol>
 *   <li>Si hay un dato vigente (más nuevo que {@code ttl.fresh}) se devuelve desde la caché.</li>
 *   <li>Si no, se consulta el servicio externo. Solo una respuesta adecuada se guarda en caché
 *       (se conserva {@code ttl.stale} como respaldo).</li>
 *   <li>Si el servicio externo falla, se devuelve el último dato bueno marcándolo como desactualizado.
 *       Si no existe ninguno, se propaga el error (503).</li>
 * </ol>
 * Los errores de negocio (no encontrado, no es persona natural, etc.) no se cachean y no activan el respaldo.
 */
@Component
public class ResilientCache {

    private static final Logger log = LoggerFactory.getLogger(ResilientCache.class);

    private final CacheStore store;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ResilientCache(CacheStore store, ObjectMapper mapper, Optional<Clock> clock) {
        this.store = store;
        this.mapper = mapper;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    public <T> CachedResult<T> get(String key, Class<T> type, AppProperties.Ttl ttl, Supplier<T> fetcher) {
        Instant now = clock.instant();
        Optional<Entry<T>> cached = read(key, type);

        if (cached.isPresent()) {
            Instant vigenteHasta = cached.get().cachedAt().plus(ttl.fresh());
            if (now.isBefore(vigenteHasta)) {
                return new CachedResult<>(cached.get().data(), CachedResult.Fuente.CACHE,
                        cached.get().cachedAt(), vigenteHasta, false, null);
            }
        }

        try {
            T fresh = fetcher.get();
            write(key, fresh, now, ttl);
            return new CachedResult<>(fresh, CachedResult.Fuente.EXTERNO, now, now.plus(ttl.fresh()), false, null);
        } catch (ExternalServiceException ex) {
            if (cached.isEmpty()) {
                throw ex;
            }
            Entry<T> last = cached.get();
            log.warn("{} falló ({}); se responde con el último dato bueno de {}", ex.getServicio(), ex.getMessage(), last.cachedAt());
            String aviso = "El servicio " + ex.getServicio() + " no está disponible. Se muestra el último dato válido obtenido el "
                    + last.cachedAt() + " (UTC); puede no estar actualizado.";
            return new CachedResult<>(last.data(), CachedResult.Fuente.CACHE_RESPALDO,
                    last.cachedAt(), last.cachedAt().plus(ttl.fresh()), true, aviso);
        }
    }

    private <T> Optional<Entry<T>> read(String key, Class<T> type) {
        return store.get(key).flatMap(json -> {
            try {
                JsonNode node = mapper.readTree(json);
                Instant cachedAt = Instant.parse(node.get("cachedAt").asText());
                T data = mapper.treeToValue(node.get("data"), type);
                return Optional.of(new Entry<>(data, cachedAt));
            } catch (Exception e) {
                log.warn("Entrada de caché corrupta para '{}', se ignora: {}", key, e.getMessage());
                return Optional.empty();
            }
        });
    }

    private <T> void write(String key, T data, Instant now, AppProperties.Ttl ttl) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("cachedAt", now.toString());
            node.set("data", mapper.valueToTree(data));
            // El TTL físico en Redis es el de respaldo: el dato sigue disponible aunque ya no sea "fresco".
            store.put(key, mapper.writeValueAsString(node), ttl.stale());
        } catch (Exception e) {
            log.warn("No se pudo guardar '{}' en caché: {}", key, e.getMessage());
        }
    }

    private record Entry<T>(T data, Instant cachedAt) {}
}
