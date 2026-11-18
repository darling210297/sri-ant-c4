package ec.edu.udla.sriant.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Datos principales del vehículo + respuesta completa del SRI en {@code detalle}. */
public record Vehiculo(
        String placa,
        String marca,
        String modelo,
        String anio,
        String clase,
        String servicio,
        String color,
        String cilindraje,
        String pais,
        String canton,
        String fechaUltimaMatricula,
        String fechaCaducidadMatricula,
        JsonNode detalle
) {}
