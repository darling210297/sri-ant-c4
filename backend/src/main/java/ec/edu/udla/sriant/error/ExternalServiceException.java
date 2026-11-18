package ec.edu.udla.sriant.error;

import org.springframework.http.HttpStatus;

/**
 * El servicio externo (SRI / ANT) no respondió o respondió algo inválido.
 * Es la única excepción que activa el respaldo con el último dato bueno de la caché.
 */
public class ExternalServiceException extends ApiException {

    private final String servicio;

    public ExternalServiceException(String servicio, String codigo, String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, codigo, mensaje);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
