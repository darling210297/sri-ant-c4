package ec.edu.udla.sriant.model;

/**
 * @param puntos          puntos actuales de la licencia (null si la ANT no los informó en la respuesta)
 * @param totalCitaciones número de citaciones registradas
 */
public record PuntosLicencia(
        String cedula,
        String placa,
        Integer puntos,
        int totalCitaciones
) {}
