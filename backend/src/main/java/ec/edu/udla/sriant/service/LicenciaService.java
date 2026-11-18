package ec.edu.udla.sriant.service;

import ec.edu.udla.sriant.cache.CachedResult;
import ec.edu.udla.sriant.cache.ResilientCache;
import ec.edu.udla.sriant.client.AntClient;
import ec.edu.udla.sriant.config.AppProperties;
import ec.edu.udla.sriant.model.PuntosLicencia;
import org.springframework.stereotype.Service;

/** Microservicio lógico: puntos de licencia (ANT) protegido con caché por su baja disponibilidad. */
@Service
public class LicenciaService {

    private final AntClient ant;
    private final ResilientCache cache;
    private final AppProperties.Ttl ttl;

    public LicenciaService(AntClient ant, ResilientCache cache, AppProperties props) {
        this.ant = ant;
        this.cache = cache;
        this.ttl = props.cache().puntosLicencia();
    }

    public CachedResult<PuntosLicencia> consultarPuntos(String cedula, String placaEntrada) {
        IdentificacionValidator.validarCedula(cedula);
        String placa = (placaEntrada == null || placaEntrada.isBlank())
                ? "" : IdentificacionValidator.normalizarPlaca(placaEntrada);
        String key = "ant:puntos:" + cedula + (placa.isEmpty() ? "" : ":" + placa);
        return cache.get(key, PuntosLicencia.class, ttl, () -> ant.consultarPuntos(cedula, placa));
    }
}
