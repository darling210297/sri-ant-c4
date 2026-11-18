package ec.edu.udla.sriant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ExternalServiceException;
import ec.edu.udla.sriant.model.PuntosLicencia;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integración con el portal web de la ANT (baja disponibilidad).
 * La ANT no tiene una API REST formal: se consulta la página de citaciones y se interpreta la respuesta
 * (JSON de jqGrid o HTML). Si la respuesta no es "adecuada" se lanza ExternalServiceException,
 * de modo que NO se guarda en caché y se responde con el último dato bueno.
 */
@Component
public class AntClient {

    public static final String SERVICIO = "ANT";
    private static final Pattern PUNTOS = Pattern.compile("(?i)puntos?[^0-9]{0,40}?(\\d{1,2})(?!\\d)");

    private final RestClient http;
    private final ObjectMapper mapper;
    private final AppProperties.Ant cfg;

    public AntClient(RestClient externalRestClient, ObjectMapper mapper, AppProperties props) {
        this.http = externalRestClient;
        this.mapper = mapper;
        this.cfg = props.ant();
    }

    public PuntosLicencia consultarPuntos(String cedula, String placa) {
        String body;
        try {
            body = http.get()
                    .uri(cfg.baseUrl() + cfg.citacionesPath()
                                    + "?ps_tipo_identificacion=CED&ps_identificacion={ced}&ps_placa={placa}",
                            cedula, placa == null ? "" : placa)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException(SERVICIO, "ANT_NO_DISPONIBLE",
                    "No se pudo obtener información de la ANT (" + ex.getClass().getSimpleName() + ").");
        }
        if (body == null || body.isBlank()) {
            throw new ExternalServiceException(SERVICIO, "ANT_RESPUESTA_INVALIDA", "La ANT devolvió una respuesta vacía.");
        }
        return parse(cedula, placa, body.trim());
    }

    PuntosLicencia parse(String cedula, String placa, String body) {
        if (body.startsWith("{")) {
            try {
                JsonNode json = mapper.readTree(body);
                JsonNode rows = json.path("rows");
                Integer puntos = findPuntos(json.toString());
                int total = rows.isArray() ? rows.size() : json.path("records").asInt(0);
                if (rows.isArray() || json.has("records") || puntos != null) {
                    return new PuntosLicencia(cedula, placa, puntos, total);
                }
            } catch (Exception ignored) {
                // cae al error de abajo
            }
            throw new ExternalServiceException(SERVICIO, "ANT_RESPUESTA_INVALIDA", "La ANT devolvió un JSON no reconocido.");
        }

        Document doc = Jsoup.parse(body);
        String text = doc.text();
        if (text.toLowerCase().matches(".*(service unavailable|error 50\\d|exception).*")) {
            throw new ExternalServiceException(SERVICIO, "ANT_NO_DISPONIBLE", "El portal de la ANT reporta un error.");
        }
        Integer puntos = findPuntos(text);
        int filas = Math.max(0, doc.select("table tr:has(td)").size());
        if (puntos == null && doc.select("table").isEmpty()) {
            throw new ExternalServiceException(SERVICIO, "ANT_RESPUESTA_INVALIDA", "No se reconoció la respuesta de la ANT.");
        }
        return new PuntosLicencia(cedula, placa, puntos, filas);
    }

    private Integer findPuntos(String text) {
        Matcher m = PUNTOS.matcher(text);
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            if (value >= 0 && value <= 30) { // la licencia en Ecuador tiene 30 puntos
                return value;
            }
        }
        return null;
    }
}
