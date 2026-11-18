package ec.edu.udla.sriant.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.edu.udla.sriant.cache.CachedResult;
import ec.edu.udla.sriant.cache.ResilientCache;
import ec.edu.udla.sriant.client.SriClient;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.error.ApiException;
import ec.edu.udla.sriant.model.Vehiculo;
import org.springframework.stereotype.Service;

/** Microservicio lógico: información del vehículo por placa (SRI matriculación vehicular). */
@Service
public class VehiculoService {

    private final SriClient sri;
    private final ResilientCache cache;
    private final AppProperties.Ttl ttl;

    public VehiculoService(SriClient sri, ResilientCache cache, AppProperties props) {
        this.sri = sri;
        this.cache = cache;
        this.ttl = props.cache().vehiculo();
    }

    public CachedResult<Vehiculo> consultar(String placaEntrada) {
        String placa = IdentificacionValidator.normalizarPlaca(placaEntrada);
        return cache.get("sri:vehiculo:" + placa, Vehiculo.class, ttl, () -> fetch(placa));
    }

    private Vehiculo fetch(String placa) {
        JsonNode n = sri.obtenerVehiculo(placa).orElseThrow(() ->
                ApiException.notFound("VEHICULO_NO_ENCONTRADO", "No existe un vehículo registrado con la placa " + placa + "."));
        return new Vehiculo(
                firstNonNull(Json.text(n, "numeroPlaca", "placa"), placa),
                Json.text(n, "descripcionMarca", "marca"),
                Json.text(n, "descripcionModelo", "modelo"),
                Json.text(n, "anioAuto", "anio", "anioModelo"),
                Json.text(n, "nombreClase", "descripcionClase", "clase"),
                Json.text(n, "descripcionServicio", "servicio"),
                Json.text(n, "colorVehiculo1", "color"),
                Json.text(n, "cilindraje"),
                Json.text(n, "descripcionPais", "pais"),
                Json.text(n, "descripcionCanton", "cantonMatricula", "canton"),
                Json.text(n, "fechaUltimaMatricula"),
                Json.text(n, "fechaCaducidadMatricula"),
                n
        );
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
