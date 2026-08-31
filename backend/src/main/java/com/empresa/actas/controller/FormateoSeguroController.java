package com.empresa.actas.controller;

import com.empresa.actas.dto.request.FormateoSeguroRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.service.FormateoSeguroService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para la generación del acta de formateo seguro.
 *
 * Endpoint:
 * - POST /generar-formateo-seguro → Genera acta de formateo, retorna nombre del ZIP.
 *
 * Reutiliza el endpoint de descarga /descargar-acta/{zip}.
 * La generación delega completamente a FormateoSeguroService.
 */
@RestController
public class FormateoSeguroController {

    private final FormateoSeguroService formateoSeguroService;

    public FormateoSeguroController(FormateoSeguroService formateoSeguroService) {
        this.formateoSeguroService = formateoSeguroService;
    }

    /**
     * Genera el acta de formateo seguro.
     *
     * @param request Datos del acta validados con @Valid.
     * @return ActaResponse con success y nombre_zip, o error.
     */
    @PostMapping("/generar-formateo-seguro")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ActaResponse generarFormateoSeguro(@Valid @RequestBody FormateoSeguroRequest request) {
        return formateoSeguroService.generarFormateoSeguro(request);
    }
}