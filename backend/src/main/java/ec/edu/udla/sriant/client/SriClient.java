package ec.edu.udla.sriant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ExternalServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/** Integración con los servicios REST públicos del SRI (catastro y matriculación vehicular). */
@Component
public class SriClient {

    public static final String SERVICIO = "SRI";

    private final RestClient http;
    private final ObjectMapper mapper;
    private final AppProperties.Sri cfg;

    public SriClient(RestClient externalRestClient, ObjectMapper mapper, AppProperties props) {
        this.http = externalRestClient;
        this.mapper = mapper;
        this.cfg = props.sri();
    }

    /** GET existePorNumeroRuc?numeroRuc= → true / false */
    public boolean existeContribuyente(String ruc) {
        String body = call(cfg.baseUrl() + cfg.existePath() + "?numeroRuc={ruc}", ruc).orElse("");
        String value = body.trim().replace("\"", "");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw invalid("respuesta inesperada al verificar el RUC");
        }
        return Boolean.parseBoolean(value);
    }

    /** GET obtenerPorNumerosRuc?&ruc= → arreglo JSON con los datos del contribuyente. */
    public Optional<JsonNode> obtenerContribuyente(String ruc) {
        return call(cfg.baseUrl() + cfg.obtenerPath() + "?ruc={ruc}", ruc)
                .map(this::parse)
                .map(n -> n.isArray() ? (n.isEmpty() ? null : n.get(0)) : n)
                .filter(n -> !n.isNull() && !n.isMissingNode());
    }

    /** GET obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn= */
    public Optional<JsonNode> obtenerVehiculo(String placa) {
        return call(cfg.baseUrl() + cfg.vehiculoPath() + "?numeroPlacaCampvCpn={placa}", placa)
                .map(this::parse)
                .filter(n -> n.isObject() && !n.isEmpty());
    }

    /**
     * Ejecuta la llamada. 404/204/cuerpo vacío → Optional.empty (no existe).
     * Timeout, error de red o 5xx → ExternalServiceException (activa el respaldo de caché).
     */
    private Optional<String> call(String uriTemplate, String value) {
        try {
            String body = http.get().uri(uriTemplate, value)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ExternalServiceException(SERVICIO, "SRI_NO_DISPONIBLE",
                                "El SRI respondió con error " + res.getStatusCode().value() + ".");
                    })
                    .body(String.class);
            return Optional.ofNullable(body).filter(b -> !b.isBlank());
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new ExternalServiceException(SERVICIO, "SRI_NO_DISPONIBLE",
                    "El SRI respondió con estado " + ex.getStatusCode().value() + ".");
        } catch (RestClientException ex) {
            throw new ExternalServiceException(SERVICIO, "SRI_NO_DISPONIBLE",
                    "No se pudo conectar con el SRI (" + ex.getClass().getSimpleName() + ").");
        }
    }

    private JsonNode parse(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw invalid("el SRI devolvió un contenido que no es JSON");
        }
    }

    private ExternalServiceException invalid(String detalle) {
        return new ExternalServiceException(SERVICIO, "SRI_RESPUESTA_INVALIDA", "Respuesta inválida del SRI: " + detalle + ".");
    }
}
