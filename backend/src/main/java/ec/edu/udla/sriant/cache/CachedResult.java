package ec.edu.udla.sriant.cache;

import java.time.Instant;

/**
 * Resultado de una consulta con metadatos de origen.
 *
 * @param fuente       EXTERNO (servicio en vivo), CACHE (dato vigente en caché) o CACHE_RESPALDO (último dato bueno; servicio caído)
 * @param obtenidoEn   cuándo se obtuvo el dato del servicio externo
 * @param vigenteHasta hasta cuándo se considera reciente
 * @param desactualizado true si el dato ya superó su tiempo de vigencia
 * @param aviso        mensaje para el usuario cuando el dato no es reciente
 */
public record CachedResult<T>(
        T data,
        Fuente fuente,
        Instant obtenidoEn,
        Instant vigenteHasta,
        boolean desactualizado,
        String aviso
) {
    public enum Fuente { EXTERNO, CACHE, CACHE_RESPALDO }
}
