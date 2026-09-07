package com.empresa.actas.acta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para crear un nuevo acta")
public record CrearActaRequest(
        @Schema(description = "Numero de ticket GLPI asociado", example = "12345")
        Long ticketGlpi,

        @NotBlank(message = "La fecha es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "La fecha debe tener formato AAAA-MM-DD (ej. 2026-08-31)")
        @Schema(description = "Fecha del acta en formato AAAA-MM-DD", example = "2026-08-31", requiredMode = Schema.RequiredMode.REQUIRED)
        String fecha,

        @NotBlank(message = "El tipo de acta es obligatorio")
        @Schema(description = "Tipo de acta", example = "ENTREGA", allowableValues = {"ENTREGA", "DEVOLUCION"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String tipoActa,

        @Size(max = 20, message = "La cedula no puede exceder 20 caracteres")
        @Schema(description = "Cedula del usuario receptor", example = "1234567890")
        String cedulaUsuario,

        @NotBlank(message = "El nombre del usuario es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        @Schema(description = "Nombre completo del usuario receptor", example = "Carlos Perez", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombreUsuario,

        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        @Email(message = "El correo no es valido")
        @Schema(description = "Correo del usuario receptor", example = "cperez@empresa.com")
        String correoUsuario,

        @Size(max = 50, message = "El serial no puede exceder 50 caracteres")
        @Schema(description = "Serial del equipo", example = "SN-2024-001")
        String serialEquipo,

        @Size(max = 50, message = "La placa no puede exceder 50 caracteres")
        @Schema(description = "Placa del equipo", example = "PL-001")
        String placaEquipo,

        @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
        @Schema(description = "Descripcion del equipo", example = "Laptop Dell Latitude 5540")
        String descripcionEquipo,

        // SEC-101: rutaPdf se ELIMINO del request. La ruta al PDF es un dato de
        // servidor (la fija la generacion documental al guardar en uploads/pdf/);
        // confiar en el cliente para ella habilito el path traversal leido en la
        // auditoria (un acta con rutaPdf="uploads/../../backend/.env" servia ese
        // archivo via /uploads o /firma). Ahora el backend siempre la deriva.
        @Schema(description = "JSON con los datos originales usados para generar el DOCX (para regeneracion del documento firmado con imagenes); lo genera el servidor al persistir la generacion, no es entrada directa del cliente en rutas actuales")
        @Size(max = 100000, message = "Los datos originales no pueden exceder 100000 caracteres")
        String datosOriginales
) {}
