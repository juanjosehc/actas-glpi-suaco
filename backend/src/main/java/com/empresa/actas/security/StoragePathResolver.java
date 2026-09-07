package com.empresa.actas.security;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SEC-101: resolucion segura de rutas virtuales "uploads/..." bajo el storage root.
 *
 * Contenimiento lexico (toAbsolutePath + normalize + startsWith): cualquier ruta
 * que escape del directorio base (por "..", por separadores alternos o por ruta
 * absoluta) devuelve null. No se confia en el prefijo "uploads/" del llamador;
 * este es unico punto de resolucion de rutas virtuales del storage.
 */
public final class StoragePathResolver {

    private StoragePathResolver() {
    }

    /**
     * Resuelve {@code rutaVirtual} que debe comenzar por {@code uploads/} bajo
     * {@code uploadsDir}. Devuelve la ruta fisica normalizada si queda contenida
     * en el directorio base, o null si el prefijo no aplica o la ruta escapa.
     */
    public static Path bajoUploads(String uploadsDir, String rutaVirtual) {
        if (rutaVirtual == null || !rutaVirtual.startsWith("uploads/")) {
            return null;
        }
        Path base = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path archivo = base.resolve(rutaVirtual.substring("uploads/".length())).normalize();
        if (!archivo.startsWith(base)) {
            return null;
        }
        return archivo;
    }
}