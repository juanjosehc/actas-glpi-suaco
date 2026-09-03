package com.empresa.actas.acta.entity;

public enum EstadoActa {
    /** Persistida y sin documentos todavia (generacion async en curso). */
    GENERANDO_DOCUMENTOS,
    /** Generacion de documentos fallo (DOCX/ZIP/PDF). Terminal: no enviable. */
    GENERACION_FALLIDA,
    GENERADA,
    ENVIADA,
    FIRMADA,
    APROBADA,
    RECHAZADA
}
