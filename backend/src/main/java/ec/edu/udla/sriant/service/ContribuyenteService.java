package ec.edu.udla.sriant.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.edu.udla.sriant.cache.CachedResult;
import ec.edu.udla.sriant.cache.ResilientCache;
import ec.edu.udla.sriant.client.SriClient;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ApiException;
import ec.edu.udla.sriant.model.Contribuyente;
import org.springframework.stereotype.Service;

/** Microservicio lógico: verificación de contribuyente y persona natural (SRI). */
@Service
public class ContribuyenteService {

    private static final String PERSONA_NATURAL = "PERSONA NATURAL";

    private final SriClient sri;
    private final ResilientCache cache;
    private final AppProperties.Ttl ttl;

    public ContribuyenteService(SriClient sri, ResilientCache cache, AppProperties props) {
        this.sri = sri;
        this.cache = cache;
        this.ttl = props.cache().contribuyente();
    }

    public CachedResult<Contribuyente> consultar(String ruc) {
        IdentificacionValidator.validarRucPersonaNatural(ruc);
        return cache.get("sri:contribuyente:" + ruc, Contribuyente.class, ttl, () -> fetch(ruc));
    }

    private Contribuyente fetch(String ruc) {
        if (!sri.existeContribuyente(ruc)) {
            throw ApiException.notFound("NO_ES_CONTRIBUYENTE", "El RUC " + ruc + " no está registrado como contribuyente en el SRI.");
        }
        JsonNode n = sri.obtenerContribuyente(ruc).orElseThrow(() ->
                ApiException.notFound("NO_ES_CONTRIBUYENTE", "El SRI no devolvió información para el RUC " + ruc + "."));

        String tipo = Json.text(n, "tipoContribuyente");
        if (tipo == null || !tipo.toUpperCase().contains(PERSONA_NATURAL)) {
            throw ApiException.unprocessable("NO_ES_PERSONA_NATURAL",
                    "El RUC pertenece a un contribuyente de tipo '" + (tipo == null ? "desconocido" : tipo) + "', no a una persona natural.");
        }

        return new Contribuyente(
                Json.text(n, "numeroRuc") != null ? Json.text(n, "numeroRuc") : ruc,
                ruc.substring(0, 10),
                Json.text(n, "razonSocial"),
                tipo,
                Json.text(n, "estadoContribuyenteRuc"),
                Json.text(n, "regimen"),
                Json.text(n, "categoria"),
                Json.text(n, "actividadEconomicaPrincipal"),
                Json.text(n, "obligadoLlevarContabilidad"),
                Json.text(n, "agenteRetencion"),
                Json.text(n, "contribuyenteEspecial"),
                Json.text(n.path("informacionFechasContribuyente"), "fechaInicioActividades")
        );
    }
}
