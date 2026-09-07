package com.empresa.actas.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO que representa un elemento de hardware en el acta de entrega.
 *
 * Cada item contiene tres campos: tipo, descripción y programa.
 * Se mapea a las variables del template Word con el prefijo "hw_N_" donde N
 * es el número de hardware (1-11).
 */
@Data
public class HardwareItem {

    // SEC-113: tamaño acotado por item; los items se embeben en el DOCX.
    @Size(max = 200, message = "tipo de hardware excede 200 caracteres")
    private String tipo = "";

    @Size(max = 500, message = "descripcion de hardware excede 500 caracteres")
    private String descripcion = "";

    @Size(max = 500, message = "programa de hardware excede 500 caracteres")
    private String programa = "";
}
