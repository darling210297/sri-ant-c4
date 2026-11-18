package ec.edu.udla.sriant.web;

import ec.edu.udla.sriant.model.ApiResponse;
import ec.edu.udla.sriant.model.Contribuyente;
import ec.edu.udla.sriant.model.PuntosLicencia;
import ec.edu.udla.sriant.model.Vehiculo;
import ec.edu.udla.sriant.service.ContribuyenteService;
import ec.edu.udla.sriant.service.LicenciaService;
import ec.edu.udla.sriant.service.VehiculoService;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato de la API (solo lectura → GET, recursos en plural, versión en la ruta):
 * <pre>
 * GET /api/v1/contribuyentes/{ruc}                 200 | 400 | 404 NO_ES_CONTRIBUYENTE | 422 NO_ES_PERSONA_NATURAL | 503
 * GET /api/v1/vehiculos/{placa}                    200 | 400 | 404 VEHICULO_NO_ENCONTRADO | 503
 * GET /api/v1/licencias/{cedula}/puntos?placa=     200 | 400 | 503
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
public class ConsultaController {

    private final ContribuyenteService contribuyentes;
    private final VehiculoService vehiculos;
    private final LicenciaService licencias;

    public ConsultaController(ContribuyenteService contribuyentes, VehiculoService vehiculos, LicenciaService licencias) {
        this.contribuyentes = contribuyentes;
        this.vehiculos = vehiculos;
        this.licencias = licencias;
    }

    @GetMapping("/contribuyentes/{ruc}")
    public ApiResponse<Contribuyente> contribuyente(
            @PathVariable @Pattern(regexp = "\\d{13}", message = "El RUC debe tener 13 dígitos numéricos.") String ruc) {
        return ApiResponse.of(contribuyentes.consultar(ruc));
    }

    @GetMapping("/vehiculos/{placa}")
    public ApiResponse<Vehiculo> vehiculo(
            @PathVariable @Pattern(regexp = "(?i)[A-Z]{2,3}-?\\d{3,4}[A-Z]?", message = "Formato de placa inválido (ej. ABC1234).") String placa) {
        return ApiResponse.of(vehiculos.consultar(placa));
    }

    @GetMapping("/licencias/{cedula}/puntos")
    public ApiResponse<PuntosLicencia> puntos(
            @PathVariable @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos numéricos.") String cedula,
            @RequestParam(required = false) String placa) {
        return ApiResponse.of(licencias.consultarPuntos(cedula, placa));
    }
}
