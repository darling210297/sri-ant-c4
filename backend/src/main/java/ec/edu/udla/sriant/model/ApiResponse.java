package ec.edu.udla.sriant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import ec.edu.udla.sriant.cache.CachedResult;

import java.time.Instant;

/** Envoltura de respuesta exitosa: el dato y de dónde salió. */
public record ApiResponse<T>(T data, Meta meta) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Meta(
            CachedResult.Fuente fuente,
            boolean desactualizado,
            Instant obtenidoEn,
            Instant vigenteHasta,
            String aviso
    ) {}

    public static <T> ApiResponse<T> of(CachedResult<T> r) {
        return new ApiResponse<>(r.data(),
                new Meta(r.fuente(), r.desactualizado(), r.obtenidoEn(), r.vigenteHasta(), r.aviso()));
    }
}
