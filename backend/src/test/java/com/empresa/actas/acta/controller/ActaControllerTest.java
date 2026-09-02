package com.empresa.actas.acta.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SEC-013: el parametro de orden de /actas pasa por una whitelist de
 * propiedades reales de la entidad; cualquier otro valor cae al default
 * en vez de lanzar InvalidDataAccessApiUsageException (500) ni abrir una
 * propiedad arbitraria al ORM.
 */
class ActaControllerTest {

    @Test
    void campoValidoDeLaWhitelistSeConserva() {
        assertEquals("fechaCreacion", ActaController.sortPermitido("fechaCreacion"));
        assertEquals("idTecnico", ActaController.sortPermitido("idTecnico"));
        assertEquals("estado", ActaController.sortPermitido("estado"));
    }

    @Test
    void campoInvalidoONullCaenAlDefault() {
        assertEquals(ActaController.SORT_DEFAULT, ActaController.sortPermitido("union;select"));
        assertEquals(ActaController.SORT_DEFAULT, ActaController.sortPermitido("id"));
        assertEquals(ActaController.SORT_DEFAULT, ActaController.sortPermitido("getDatosOriginales"));
        assertEquals(ActaController.SORT_DEFAULT, ActaController.sortPermitido(null));
        assertEquals(ActaController.SORT_DEFAULT, ActaController.sortPermitido(""));
    }
}