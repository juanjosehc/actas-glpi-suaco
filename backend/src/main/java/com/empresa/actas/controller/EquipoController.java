package com.empresa.actas.controller;

import com.empresa.actas.dto.response.EquipoResponse;
import com.empresa.actas.service.EquipoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para la consulta de equipos desde GLPI.
 *
 * Endpoint:
 * - GET /equipo/{serial} → Busca equipo por serial y retorna marca, tipo y modelo.
 *
 * Utilizado por el frontend para auto completar datos del equipo
 * al hacer click en el botón "Buscar" dentro de cada bloque de equipo.
 */
@RestController
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    /**
     * Consulta un equipo en GLPI por su número de serial.
     *
     * @param serial Número de serial del equipo a buscar.
     * @return EquipoResponse con marca, tipo y modelo (vacíos si no se encuentra).
     */
    @GetMapping("/equipo/{serial}")
    public EquipoResponse obtenerEquipo(@PathVariable String serial) {
        // SEC-005: el endpoint es publico y el serial viaja a GLPI. La validacion
        // en la frontera de entrada (allow-list + longitud) rechaza seriales
        // malformados o con caracteres de inyeccion antes de tocar cualquier
        // servicio externo. Se lanza IllegalArgumentException porque el
        // GlobalExceptionHandler la traduce a 400 con el mensaje redactado.
        if (!EquipoService.serialValido(serial)) {
            throw new IllegalArgumentException(
                    "Serial invalido: solo letras, numeros y . _ - (1 a 64 caracteres)");
        }
        return equipoService.buscarEquipo(serial);
    }
}
