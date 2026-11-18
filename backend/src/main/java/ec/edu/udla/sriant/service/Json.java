package ec.edu.udla.sriant.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Lectura tolerante de campos: los servicios del gobierno cambian nombres o devuelven null. */
final class Json {

    private Json() {}

    /** Devuelve el primer campo no vacío de la lista de nombres alternativos. */
    static String text(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            JsonNode v = node.get(name);
            if (v != null && !v.isNull() && !v.asText().isBlank()) {
                return v.asText().trim();
            }
        }
        return null;
    }
}
