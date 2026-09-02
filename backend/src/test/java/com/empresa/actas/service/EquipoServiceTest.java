package com.empresa.actas.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-005: la allow-list del serial es la primera barrera del endpoint publico
 * /equipo/{serial}. Los seriales legitimos pasan; caracteres de inyeccion
 * (parametros URL, separadores, markup) o longitudes abusivas se rechazan.
 */
class EquipoServiceTest {

    @Test
    void serialesLegitimosSiguenSiendoValidos() {
        assertTrue(EquipoService.serialValido("ABC123"));
        assertTrue(EquipoService.serialValido("abc-xyz_123"));
        assertTrue(EquipoService.serialValido("SN.2024-08"));
        assertTrue(EquipoService.serialValido("c2c0071yhu"));
        assertTrue(EquipoService.serialValido("a".repeat(64)));
    }

    @Test
    void inyeccionDeParametrosODelimitadoresEsRechazada() {
        assertFalse(EquipoService.serialValido("X&criteria[0][value]=Y"));
        assertFalse(EquipoService.serialValido("X;DROP TABLE acta--"));
        assertFalse(EquipoService.serialValido("X|grep /etc/passwd"));
        assertFalse(EquipoService.serialValido("<script>alert(1)</script>"));
        assertFalse(EquipoService.serialValido("X' OR '1'='1"));
    }

    @Test
    void vacioBlancosYLongitudesAbusivasSonRechazados() {
        assertFalse(EquipoService.serialValido(null));
        assertFalse(EquipoService.serialValido(""));
        assertFalse(EquipoService.serialValido("   "));
        assertFalse(EquipoService.serialValido("a".repeat(65)));
    }
}