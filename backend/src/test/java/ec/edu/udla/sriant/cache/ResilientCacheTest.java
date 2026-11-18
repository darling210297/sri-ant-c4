package ec.edu.udla.sriant.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ApiException;
import ec.edu.udla.sriant.error.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilientCacheTest {

    record Dato(String valor) {}

    static class MemoryStore implements CacheStore {
        final Map<String, String> map = new HashMap<>();
        public Optional<String> get(String key) { return Optional.ofNullable(map.get(key)); }
        public void put(String key, String value, Duration ttl) { map.put(key, value); }
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-17T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    private final AppProperties.Ttl ttl = new AppProperties.Ttl(Duration.ofHours(1), Duration.ofDays(3));
    private MemoryStore store;
    private MutableClock clock;
    private ResilientCache cache;

    @BeforeEach
    void setUp() {
        store = new MemoryStore();
        clock = new MutableClock();
        cache = new ResilientCache(store, new ObjectMapper(), Optional.of(clock));
    }

    @Test
    void primeraConsultaVaAlServicioYGuardaEnCache() {
        CachedResult<Dato> r = cache.get("k", Dato.class, ttl, () -> new Dato("A"));
        assertEquals(CachedResult.Fuente.EXTERNO, r.fuente());
        assertEquals("A", r.data().valor());
        assertTrue(store.map.containsKey("k"));
    }

    @Test
    void datoVigenteSeDevuelveDesdeCacheSinLlamarAlServicio() {
        AtomicInteger llamadas = new AtomicInteger();
        cache.get("k", Dato.class, ttl, () -> { llamadas.incrementAndGet(); return new Dato("A"); });
        clock.now = clock.now.plus(Duration.ofMinutes(30));
        CachedResult<Dato> r = cache.get("k", Dato.class, ttl, () -> { llamadas.incrementAndGet(); return new Dato("B"); });
        assertEquals(CachedResult.Fuente.CACHE, r.fuente());
        assertEquals("A", r.data().valor());
        assertEquals(1, llamadas.get());
    }

    @Test
    void siElServicioFallaDevuelveUltimoDatoBuenoYAvisa() {
        cache.get("k", Dato.class, ttl, () -> new Dato("A"));
        clock.now = clock.now.plus(Duration.ofHours(5)); // ya no es fresco
        CachedResult<Dato> r = cache.get("k", Dato.class, ttl, () -> {
            throw new ExternalServiceException("ANT", "ANT_NO_DISPONIBLE", "caído");
        });
        assertEquals(CachedResult.Fuente.CACHE_RESPALDO, r.fuente());
        assertTrue(r.desactualizado());
        assertNotNull(r.aviso());
        assertEquals("A", r.data().valor());
    }

    @Test
    void siElServicioFallaYNoHayCacheSePropagaElError() {
        assertThrows(ExternalServiceException.class, () -> cache.get("k", Dato.class, ttl, () -> {
            throw new ExternalServiceException("ANT", "ANT_NO_DISPONIBLE", "caído");
        }));
    }

    @Test
    void erroresDeNegocioNoSeCacheanNiUsanRespaldo() {
        assertThrows(ApiException.class, () -> cache.get("k", Dato.class, ttl, () -> {
            throw ApiException.notFound("NO_ES_CONTRIBUYENTE", "no existe");
        }));
        assertFalse(store.map.containsKey("k"));
    }

    @Test
    void datoVencidoSeRefrescaCuandoElServicioResponde() {
        cache.get("k", Dato.class, ttl, () -> new Dato("A"));
        clock.now = clock.now.plus(Duration.ofHours(2));
        CachedResult<Dato> r = cache.get("k", Dato.class, ttl, () -> new Dato("B"));
        assertEquals(CachedResult.Fuente.EXTERNO, r.fuente());
        assertEquals("B", r.data().valor());
    }
}
