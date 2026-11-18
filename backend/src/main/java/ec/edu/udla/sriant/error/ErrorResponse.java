package ec.edu.udla.sriant.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Formato único de error de la API. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int estado,
        String codigo,
        String mensaje,
        String ruta,
        Instant timestamp,
        List<CampoInvalido> detalles
) {
    public record CampoInvalido(String campo, String mensaje) {}
}
