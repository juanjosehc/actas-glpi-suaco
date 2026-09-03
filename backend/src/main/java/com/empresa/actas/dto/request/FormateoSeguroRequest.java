package com.empresa.actas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para la generación del acta de formateo seguro.
 *
 * Contiene la información necesaria para generar el acta de formateo (DOCX).
 * Solo incluye los campos del template ActaFormateoSeguro.docx: datos del
 * acta y hasta 4 equipos (la plantilla indexa eq_1..eq_4).
 *
 * No incluye checklist, hardware ni observaciones (a diferencia de entrega).
 */
@Data
public class FormateoSeguroRequest {

    @NotBlank(message = "La fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener formato AAAA-MM-DD (ej. 2026-08-31)")
    private String fecha;

    private String entregado_a = "";

    /** Correo del usuario principal (entregado_por) que firma el formateo (autocompletado desde GLPI). Opcional; si viene con valor se valida formato. */
    @Email(message = "El correo no es valido")
    private String correo = "";

    private String cargo_recibe = "";

    private String entregado_por = "";

    private String cargo_entrega = "";

    private String asunto = "";

    @Size(max = 4, message = "Maximo 4 equipos por acta de formateo")
    private List<EquipoItem> equipos = new ArrayList<>();
}