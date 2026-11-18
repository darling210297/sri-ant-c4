package ec.edu.udla.sriant.cache;

import ec.edu.udla.sriant.error.ApiException;
import ec.edu.udla.sriant.service.IdentificacionValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentificacionValidatorTest {

    @Test
    void cedulas() {
        assertTrue(IdentificacionValidator.cedulaValida("1710034065"));
        assertFalse(IdentificacionValidator.cedulaValida("1710034066"));
        assertFalse(IdentificacionValidator.cedulaValida("9910034065"));
    }

    @Test
    void ruc() {
        assertDoesNotThrow(() -> IdentificacionValidator.validarRucPersonaNatural("1710034065001"));
        assertThrows(ApiException.class, () -> IdentificacionValidator.validarRucPersonaNatural("1790011674001")); // sociedad
        assertThrows(ApiException.class, () -> IdentificacionValidator.validarRucPersonaNatural("1710034065"));
    }

    @Test
    void placas() {
        assertEquals("PBX1234", IdentificacionValidator.normalizarPlaca("pbx-1234"));
        assertEquals("IB123C", IdentificacionValidator.normalizarPlaca("IB123C"));
        assertThrows(ApiException.class, () -> IdentificacionValidator.normalizarPlaca("12ABC"));
    }
}
