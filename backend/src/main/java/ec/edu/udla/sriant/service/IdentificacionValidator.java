package ec.edu.udla.sriant.service;

import ec.edu.udla.sriant.error.ApiException;

import java.util.regex.Pattern;

/** Reglas de validación de cédula, RUC de persona natural y placa (Ecuador). */
public final class IdentificacionValidator {

    private static final Pattern PLACA = Pattern.compile("^([A-Z]{3}\\d{3,4}|[A-Z]{2}\\d{3}[A-Z])$");

    private IdentificacionValidator() {}

    /** Algoritmo módulo 10 del Registro Civil. */
    public static boolean cedulaValida(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) return false;
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (!((provincia >= 1 && provincia <= 24) || provincia == 30)) return false;
        if (cedula.charAt(2) - '0' >= 6) return false;
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int d = (cedula.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += d > 9 ? d - 9 : d;
        }
        int verificador = (10 - suma % 10) % 10;
        return verificador == cedula.charAt(9) - '0';
    }

    public static void validarRucPersonaNatural(String ruc) {
        if (ruc == null || !ruc.matches("\\d{13}")) {
            throw ApiException.badRequest("RUC_INVALIDO", "El RUC debe tener 13 dígitos numéricos.");
        }
        if (!ruc.endsWith("001")) {
            throw ApiException.badRequest("RUC_INVALIDO", "El RUC de persona natural debe terminar en 001.");
        }
        if (!cedulaValida(ruc.substring(0, 10))) {
            throw ApiException.badRequest("RUC_INVALIDO",
                    "Los 10 primeros dígitos del RUC no corresponden a una cédula válida (persona natural).");
        }
    }

    public static void validarCedula(String cedula) {
        if (!cedulaValida(cedula)) {
            throw ApiException.badRequest("CEDULA_INVALIDA", "La cédula no es válida (dígito verificador incorrecto).");
        }
    }

    /** Normaliza "abc-1234" → "ABC1234" y valida el formato. */
    public static String normalizarPlaca(String placa) {
        String p = placa == null ? "" : placa.trim().toUpperCase().replace("-", "").replace(" ", "");
        if (!PLACA.matcher(p).matches()) {
            throw ApiException.badRequest("PLACA_INVALIDA", "La placa debe tener el formato ABC1234 (auto) o AB123C (moto).");
        }
        return p;
    }
}
