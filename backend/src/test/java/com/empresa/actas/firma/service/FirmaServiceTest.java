package com.empresa.actas.firma.service;

import org.junit.jupiter.api.Test;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SEC-008: la decodificacion de evidencias del portal de firma valida formato
 * (magic bytes), tamaño y Base64 antes de persistir; el fallo es 400 y nunca
 * un 500 de Base64/archivo.
 */
class FirmaServiceTest {

    /** PNG real de 1x1 (magic 89 50 4E 47 ...) */
    private static final byte[] PNG_1X1 = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    /** Magic de JPEG: FF D8 FF */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};

    private String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void pngValidoDecodifica() {
        byte[] resultado = FirmaService.decodificarEvidenciaValida(
                b64(PNG_1X1), FirmaService.TAMANO_MAX_FIRMA, FirmaService.MAGIC_PNG, "La firma");
        assertArrayEquals(PNG_1X1, resultado);
    }

    @Test
    void dataUrlConPrefijoTambienEsAceptado() {
        byte[] resultado = FirmaService.decodificarEvidenciaValida(
                "data:image/png;base64," + b64(PNG_1X1),
                FirmaService.TAMANO_MAX_FIRMA, FirmaService.MAGIC_PNG, "La firma");
        assertArrayEquals(PNG_1X1, resultado);
    }

    @Test
    void jpegConMagicJpegDecodifica() {
        byte[] resultado = FirmaService.decodificarEvidenciaValida(
                b64(JPEG), FirmaService.TAMANO_MAX_FOTO, FirmaService.MAGIC_JPEG, "La foto");
        assertArrayEquals(JPEG, resultado);
    }

    @Test
    void formatoEquivocadoEsRechazadoCon400() {
        // Mismo contenido PNG declarado como foto JPG: el magic no coincide.
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        b64(PNG_1X1), FirmaService.TAMANO_MAX_FOTO,
                        FirmaService.MAGIC_JPEG, "La foto"));
        // Payload arbitrario (texto plano) no es PNG.
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        b64("no-soy-una-imagen".getBytes(StandardCharsets.UTF_8)),
                        FirmaService.TAMANO_MAX_FIRMA, FirmaService.MAGIC_PNG, "La firma"));
    }

    @Test
    void excesoTamanoEsRechazadoCon400() {
        byte[] pesado = new byte[(int) FirmaService.TAMANO_MAX_FIRMA + 1];
        Arrays.fill(pesado, (byte) 0x89);
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        b64(pesado), FirmaService.TAMANO_MAX_FIRMA,
                        FirmaService.MAGIC_PNG, "La firma"));
    }

    @Test
    void base64InvalidaOVaciaEsRechazadaCon400() {
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        "!!!base64 !invalido!!!", FirmaService.TAMANO_MAX_FIRMA,
                        FirmaService.MAGIC_PNG, "La firma"));
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        "", FirmaService.TAMANO_MAX_FIRMA,
                        FirmaService.MAGIC_PNG, "La firma"));
        assertThrows(IllegalArgumentException.class, () ->
                FirmaService.decodificarEvidenciaValida(
                        null, FirmaService.TAMANO_MAX_FIRMA,
                        FirmaService.MAGIC_PNG, "La firma"));
    }
}