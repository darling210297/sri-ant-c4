package ec.edu.udla.sriant.model;

public record Contribuyente(
        String ruc,
        String cedula,
        String razonSocial,
        String tipoContribuyente,
        String estado,
        String regimen,
        String categoria,
        String actividadEconomicaPrincipal,
        String obligadoLlevarContabilidad,
        String agenteRetencion,
        String contribuyenteEspecial,
        String fechaInicioActividades
) {}
