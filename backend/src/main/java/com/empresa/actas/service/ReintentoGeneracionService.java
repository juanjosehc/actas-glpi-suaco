package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reintento generico de generacion documental (async).
 *
 * Dos usos del mismo dispatch:
 * 1. Re-encolado al arrancar: si la JVM murio con actas en GENERANDO_DOCUMENTOS,
 *    quedaron congeladas. Se re-encolan desde datosOriginales; las que no puedan
 *    regenerarse pasan a GENERACION_FALLIDA (terminal) para no quedar colgadas.
 * 2. Reintento manual (POST /actas/{id}/reintentar-generacion) tras una
 *    GENERACION_FALLIDA.
 */
@Component
@RequiredArgsConstructor
public class ReintentoGeneracionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReintentoGeneracionService.class);

    private final ActaRepository actaRepository;
    private final DocxActaService docxActaService;
    private final DevolucionService devolucionService;
    private final FormateoSeguroService formateoSeguroService;
    private final GeneracionDocumentalAsyncService generacionDocumentalAsyncService;

    @Override
    public void run(String... args) {
        List<Acta> congeladas = actaRepository.findByEstado(EstadoActa.GENERANDO_DOCUMENTOS);
        if (congeladas.isEmpty()) {
            return;
        }
        log.info("Re-encolando {} acta(s) en GENERANDO_DOCUMENTOS tras arranque", congeladas.size());
        congeladas.forEach(acta -> {
            try {
                dispatch(acta.getIdActa());
            } catch (Exception e) {
                // El acta no puede regenerarse (datosOriginales invalidos, tema,
                // etc.): pasarla a terminal para no dejarla colgada en el limbo.
                log.warn("Re-encolado fallo para acta {} tras arranque: {}", acta.getIdActa(), e.getMessage());
                generacionDocumentalAsyncService.marcarFallida(acta.getIdActa(), e.getMessage());
            }
        });
    }

    /**
     * Re-encola la regeneracion de un acta segun su tipo. Los errores se
     * propagan (el controller los convierte en 400/404): un reintento manual
     * que no corresponda no debe cambiar el estado de la acta.
     */
    public void reintentar(Long idActa) {
        dispatch(idActa);
    }

    private void dispatch(Long idActa) {
        Acta acta = actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada con id: " + idActa));
        switch (acta.getTipoActa()) {
            case ENTREGA -> docxActaService.reintentarGeneracion(idActa);
            case DEVOLUCION -> devolucionService.reintentarGeneracion(idActa);
            case FORMATEO -> formateoSeguroService.reintentarGeneracion(idActa);
            default -> throw new IllegalArgumentException(
                    "Tipo de acta no re-generable: " + acta.getTipoActa());
        }
    }
}