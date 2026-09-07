package com.empresa.actas.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO que representa un equipo de cómputo en una acta.
 *
 * Utilizado tanto en actas de entrega como de devolución.
 * Los campos se autocompletan desde GLPI al buscar por serial.
 * En devolución se requiere additionally el campo "estado".
 *
 * Mapeado a las variables del template Word con el prefijo "eq_N_" donde N
 * es el número de equipo (1-10).
 */
@Data
public class EquipoItem {

    // SEC-113: tamaños acotados por item; serial acotado como la frontera de
    // entrada de GLPI (1-64). El resto se embebe en el DOCX.
    @Size(max = 64, message = "serial de equipo excede 64 caracteres")
    private String serial = "";

    @Size(max = 200, message = "marca de equipo excede 200 caracteres")
    private String marca = "";

    @Size(max = 200, message = "tipo de equipo excede 200 caracteres")
    private String tipo = "";

    @Size(max = 200, message = "modelo de equipo excede 200 caracteres")
    private String modelo = "";

    @Size(max = 200, message = "inventario excede 200 caracteres")
    private String inventario = "";

    @Size(max = 200, message = "estado de equipo excede 200 caracteres")
    private String estado = "";

    @Size(max = 200, message = "gb excede 200 caracteres")
    private String gb = "";
}
