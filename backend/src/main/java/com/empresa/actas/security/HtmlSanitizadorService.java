package com.empresa.actas.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

/**
 * Sanitizacion HTML con OWASP Java HTML Sanitizer (SEC-001).
 *
 * Punto de control unico del servidor: bloquea XSS almacenado antes de
 * persistir y de exponer contenido en respuestas. El frontend SIEMPRE recibe
 * contenido ya sanitizado y los campos de texto se tratan como texto plano.
 *
 * Permitido (allowlist):
 *  - BLOCKS:      p, div, h1-h6, ul, ol, li, blockquote
 *  - FORMATTING:  b, i, u, s, em, strong, small, sub, sup, code, q, span, ...
 *  - TABLES:      table, thead, tbody, tfoot, tr, th, td, colgroup, col, caption
 *  - LINKS:       a[href] (http/https/mailto, target=_blank)
 *  - IMAGES:      img[src] (http/https)
 *
 * Todo lo demas (script, iframe, style, event handlers on*, javascript: URLs)
 * se elimina o desactiva por la politica de la libreria.
 */
@Service
public class HtmlSanitizadorService {

    private final PolicyFactory policyHtml = Sanitizers.BLOCKS
            .and(Sanitizers.FORMATTING)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES);

    /**
     * Sanitiza HTML enriquecido (contenidoHtml del acta): conserva la
     * estructura documental (tablas, parrafos, formato) y neutraliza
     * cualquier markup activo. Devuelve null si la entrada es null.
     */
    public String sanitizarHtml(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return policyHtml.sanitize(html);
    }
}