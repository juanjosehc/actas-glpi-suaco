package com.empresa.actas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta exitosa para operaciones de generación de actas.
 *
 * Utilizado tanto para actas de entrega como de devolución.
 * Contiene el nombre del ZIP generado para que el frontend
 * pueda solicitarlo vía /descargar-acta/{nombreZip}.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActaResponse {

    private boolean success;

    @JsonProperty("nombre_zip")
    private String nombreZip;

    private String mensaje;

    @JsonProperty("ruta_pdf")
    private String rutaPdf;

    @JsonProperty("id_acta")
    private Long idActa;

    /** Estado de la acta tras el POST (GENERANDO_DOCUMENTOS en flujo async). */
    private String estado;

    public ActaResponse(boolean success, String nombreZip, String mensaje, String rutaPdf) {
        this.success = success;
        this.nombreZip = nombreZip;
        this.mensaje = mensaje;
        this.rutaPdf = rutaPdf;
    }

    /**
     * Respuesta inmediata del flujo asincrono: la acta quedo persistida en
     * GENERANDO_DOCUMENTOS y los documentos (DOCX/ZIP/PDF) se generan en
     * segundo plano. El frontend redirige al listado y hace polling.
     */
    public static ActaResponse procesando(Long idActa) {
        ActaResponse r = new ActaResponse(true, null,
                "Acta registrada. Documentacion en generacion.", null);
        r.setIdActa(idActa);
        r.setEstado("GENERANDO_DOCUMENTOS");
        return r;
    }

    /**
     * Crea una respuesta exitosa con el nombre del ZIP generado.
     *
     * @param nombreZip Nombre del archivo ZIP para descarga.
     * @return ActaResponse con success=true.
     */
    public static ActaResponse ok(String nombreZip) {
        return new ActaResponse(true, nombreZip, "Documentacion generada correctamente", null);
    }

    /**
     * Crea una respuesta exitosa con el nombre del ZIP y la ruta del PDF.
     *
     * @param nombreZip Nombre del archivo ZIP para descarga.
     * @param rutaPdf   Ruta relativa del PDF generado.
     * @return ActaResponse con success=true y ruta_pdf.
     */
    public static ActaResponse ok(String nombreZip, String rutaPdf) {
        return new ActaResponse(true, nombreZip, "Documentacion generada correctamente", rutaPdf);
    }

    /**
     * Crea una respuesta exitosa con el nombre del ZIP, la ruta del PDF y
     * el id de la entidad Acta persistida en PostgreSQL.
     */
    public static ActaResponse ok(String nombreZip, String rutaPdf, Long idActa) {
        ActaResponse r = new ActaResponse(true, nombreZip, "Documentacion generada correctamente", rutaPdf);
        r.setIdActa(idActa);
        return r;
    }

    /**
     * Crea una respuesta de error con un mensaje descriptivo.
     *
     * @param mensaje Descripción del error.
     * @return ActaResponse con success=false.
     */
    public static ActaResponse error(String mensaje) {
        return new ActaResponse(false, null, mensaje, null);
    }
}
