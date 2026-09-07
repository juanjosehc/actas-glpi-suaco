package com.empresa.actas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SEC-120: capaz de digerir un token-capability (p. ej. token de recuperacion
 * de contrasena) a SHA-256 hex antes de persistirlo. El valor en BD ya no es
 * una capability usable: quien tenga un dump no puede forjar el enlace. El
 * artefacto se guarda SOLO en el correo/URL entregado al usuario.
 *
 * Se usa SHA-256 (determinista) y no BCrypt a proposito: la busqueda necesita
 * comparar el digito recibido contra el almacenado, y BCrypt usa salt
 * aleatorio que impide la consulta por igualdad. La entropia del UUID (122
 * bits) hace el hash resistente a fuerza bruta por diccionario.
 */
public final class TokenDigest {

    private TokenDigest() {
    }

    /** SHA-256 hex (64 chars) del token crudo; null -> null. */
    public static String sha256(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 pertenece al JDK estandar; nunca llega aqui.
            throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
        }
    }
}