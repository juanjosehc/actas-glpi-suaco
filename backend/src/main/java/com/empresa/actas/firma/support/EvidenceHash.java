package com.empresa.actas.firma.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes criptograficos para evidencia probatoria (FORTALECIMIENTO_EVIDENCIA_FIRMA).
 *
 * {@code sha256} produce una huella inmutable (hex, 64 chars) sin PII: sirve para
 * comprometer el correo objetivo de un OTP sin exponerlo, y para firmar la
 * evidencia de un envio SMTP. SHA-256 es unidireccional y no reversible, por lo
 * que no se considera dato personal.
 */
public final class EvidenceHash {

    private EvidenceHash() {
    }

    /** SHA-256 en hex minuscula del texto dado; null/blank -> null. */
    public static String sha256(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre existe en el JDK; solo es alcanzable por config rota.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
