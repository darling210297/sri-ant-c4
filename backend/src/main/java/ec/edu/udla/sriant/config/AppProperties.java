package ec.edu.udla.sriant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Http http,
        Sri sri,
        Ant ant,
        Cache cache
) {
    public record Cors(List<String> allowedOrigins) {}

    public record Http(int connectTimeoutMs, int readTimeoutMs) {}

    public record Sri(String baseUrl, String existePath, String obtenerPath, String vehiculoPath) {}

    public record Ant(String baseUrl, String citacionesPath) {}

    public record Cache(Ttl contribuyente, Ttl vehiculo, Ttl puntosLicencia) {}

    /** fresh: tiempo en que el dato se considera vigente; stale: tiempo máximo que se conserva como respaldo. */
    public record Ttl(Duration fresh, Duration stale) {}
}
