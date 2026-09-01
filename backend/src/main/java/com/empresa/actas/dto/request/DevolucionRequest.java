package com.empresa.actas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para la generación del acta de devolución.
 *
 * Contiene la información necesaria para generar el acta de devolución (DOCX).
 *
 * Diferencias con ActaRequest:
 * - No incluye checklist ni sistema operativo.
 * - No incluye hardware detallado (solo tipo).
 * - Incluye campos de jefe directo (nombre + cargo).
 * - Incluye campo cedula del entregador.
 *
 * Solo fecha es obligatoria con @NotBlank; los demás campos
 * se validan en el frontend antes de enviar.
 */
@Data
public class DevolucionRequest {

    @NotBlank(message = "La fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener formato AAAA-MM-DD (ej. 2026-08-31)")
    private String fecha;

    private String recibido_por = "";

    private String entregado_por = "";

    /** Correo del usuario que entrega (quien firma la devolucion, autocompletado desde GLPI). Opcional; si viene con valor se valida formato. */
    @Email(message = "El correo no es valido")
    private String correo = "";

    private String cargo_recibe = "";

    private String cedula = "";

    private String area_recibe = "";

    private String motivo = "";

    private String cargo_entrega = "";

    private String nombre_jefe = "";

    private String cargo_jefe = "";

    @Size(max = 3, message = "Maximo 3 equipos por acta de devolucion (capacidad del template)")
    private List<EquipoItem> equipos = new ArrayList<>();

    @Size(max = 3, message = "Maximo 3 otros elementos por acta de devolucion (capacidad del template)")
    private List<OtroElementoItem> hardware = new ArrayList<>();

    private String observaciones = "";
}
