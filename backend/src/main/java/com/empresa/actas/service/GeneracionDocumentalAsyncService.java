package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pone la generacion de documentos (DOCX->ZIP->PDF) fuera del request HTTP.
 *
 * Patron identico al ya usado para firmaPdfExecutor (FirmaController): un solo
 * hilo daemon serializa toda la generacion documental — ademas, LibreOffice
 * tiene un semaforo global propio, asi que dos conversiones simultaneas son
 * imposibles por construccion.
 *
 * El POST /generar-* persiste el acta en GENERANDO_DOCUMENTOS y encola aqui el
 * trabajo real; el hilo async ejecuta y cierra el ciclo con GENERADA (o
 * GENERACION_FALLIDA si algo falla), registrando el evento de auditoria.
 */
@Service
@RequiredArgsConstructor
public class GeneracionDocumentalAsyncService {

    private static final Logger log = LoggerFactory.getLogger(GeneracionDocumentalAsyncService.class);

    private final ActaRepository actaRepository;
    private final ActaHistorialService actaHistorialService;

    private final ExecutorService generacionExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "generacion-documental");
                t.setDaemon(true);
                return t;
            });

    /** Encola la tarea de generacion de documentos (daemon, un solo hilo). */
    public void encolar(Runnable tarea) {
        generacionExecutor.execute(tarea);
    }

    /**
     * Cierra el ciclo async de un acta en GENERANDO_DOCUMENTOS -> GENERADA,
     * guardando las rutas de los PDF y el nombre del ZIP ya generados.
     */
    @Transactional
    public void marcarGenerada(Long idActa, String rutaPdf, String rutaPdfChecklist, String nombreZip) {
        Acta acta = cargar(idActa);
        acta.setEstado(EstadoActa.GENERADA);
        acta.setRutaPdf(rutaPdf);
        acta.setRutaPdfChecklist(rutaPdfChecklist);
        acta.setRutaZip(nombreZip);
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                idActa,
                TipoEventoActa.ACTA_GENERADA,
                EstadoActa.GENERANDO_DOCUMENTOS,
                EstadoActa.GENERADA,
                acta.getIdTecnico(),
                actorNombre(acta),
                null,
                "Acta generada en segundo plano: " + rutaPdf
                        + (rutaPdfChecklist != null ? " ; checklist: " + rutaPdfChecklist : ""));
        log.info("Acta {} -> GENERADA (async). PDF: {}", idActa, rutaPdf);
    }

    /** Marca la generacion como fallida (terminal; no enviable). */
    @Transactional
    public void marcarFallida(Long idActa, String error) {
        Acta acta = cargar(idActa);
        acta.setEstado(EstadoActa.GENERACION_FALLIDA);
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                idActa,
                TipoEventoActa.GENERACION_FALLIDA,
                EstadoActa.GENERANDO_DOCUMENTOS,
                EstadoActa.GENERACION_FALLIDA,
                acta.getIdTecnico(),
                actorNombre(acta),
                null,
                "Falló la generación de documentos: " + error);
        log.warn("Acta {} -> GENERACION_FALLIDA: {}", idActa, error);
    }

    private Acta cargar(Long idActa) {
        return actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada con id: " + idActa));
    }

    private String actorNombre(Acta acta) {
        return acta.getIdTecnico() != null ? String.valueOf(acta.getIdTecnico()) : "SISTEMA";
    }
}