package com.empresa.actas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO de entrada para la generación del acta de entrega.
 *
 * Contiene toda la información necesaria para generar:
 * - Acta de entrega (DOCX)
 * - Lista de chequeo (DOCX)
 *
 * Campos obligatorios validados con @NotBlank:
 * - fecha, entregado_a, cargo_recibe, entregado_por,
 *   cargo_entrega, asunto, numero_sac, sistema_operativo.
 *
 * Campos opcionales con valores por defecto:
 * - hardware, equipos, checklist, observaciones.
 */
@Data
public class ActaRequest {

    @NotBlank(message = "La fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener formato AAAA-MM-DD (ej. 2026-08-31)")
    private String fecha;

    @NotBlank(message = "El campo entregado_a es obligatorio")
    private String entregado_a;

    @NotBlank(message = "El campo cargo_recibe es obligatorio")
    private String cargo_recibe;

    @NotBlank(message = "El campo entregado_por es obligatorio")
    private String entregado_por;

    /** Correo del usuario receptor (autocompletado desde GLPI). Opcional; si viene con valor se valida formato. */
    @Email(message = "El correo no es valido")
    private String correo = "";

    @NotBlank(message = "El campo cargo_entrega es obligatorio")
    private String cargo_entrega;

    @NotBlank(message = "El campo asunto es obligatorio")
    private String asunto;

    @Size(max = 9, message = "Maximo 9 registros de Hardware y Software (capacidad del template)")
    private List<HardwareItem> hardware = new ArrayList<>();

    @Size(max = 3, message = "Maximo 3 equipos por acta de entrega (capacidad del template)")
    private List<EquipoItem> equipos = new ArrayList<>();

    private Map<String, Boolean> checklist = new HashMap<>();

    @NotBlank(message = "El numero_sac es obligatorio")
    @Pattern(regexp = "\\d{1,18}", message = "El numero_sac debe contener solo numeros (maximo 18 digitos)")
    private String numero_sac;

    private String observaciones = "";

    @NotBlank(message = "El sistema operativo es obligatorio")
    private String sistema_operativo;
}
