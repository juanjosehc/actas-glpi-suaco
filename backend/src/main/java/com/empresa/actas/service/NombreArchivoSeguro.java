package com.empresa.actas.service;

/**
 * Genera segmentos de nombres de archivo seguros a partir de texto de usuario
 * (seriales, asuntos, motivos).
 *
 * QA-25: neutraliza caracteres invalidos/reservados en Windows y Linux, los
 * separadores de ruta y los valores de traversal (".."), y limita la longitud
 * para que el nombre final del archivo no exceda limites practicos del SO.
 *
 * Estrategia: reemplazar con "_" todo caracter reservado o separador, y recortar
 * a 50 caracteres. Un valor vacio/segun solo caracteres invalidos produce "_".
 */
public final class NombreArchivoSeguro {

    private static final int MAX_SEGMENTO = 50;

    // [<>:"/\\|?*] son reservados en Windows; / y \ son separadores de ruta en
    // ambos SO; \x00-\x1F\x7F son caracteres de control que Windows rechaza.
    private static final String CHAR_ILEGAL = "[<>:\"/\\\\|?*\\x00-\\x1F\\x7F]";

    private NombreArchivoSeguro() {
    }

    public static String segmento(String valor) {
        if (valor == null) {
            return "_";
        }
        String limpio = valor
                .replaceAll(CHAR_ILEGAL, "_")
                .replace("..", "_")
                .trim();
        if (limpio.isBlank()) {
            return "_";
        }
        if (limpio.length() > MAX_SEGMENTO) {
            limpio = limpio.substring(0, MAX_SEGMENTO);
        }
        return limpio;
    }
}