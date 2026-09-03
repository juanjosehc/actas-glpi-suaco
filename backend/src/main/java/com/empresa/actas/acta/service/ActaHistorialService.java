package com.empresa.actas.acta.service;

import com.empresa.actas.acta.entity.ActaHistorial;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaHistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActaHistorialService {

    private final ActaHistorialRepository actaHistorialRepository;

    @Transactional
    public void registrarEvento(Long idActa,
                                TipoEventoActa tipoEvento,
                                EstadoActa estadoAnterior,
                                EstadoActa estadoNuevo,
                                Long actorId,
                                String actorNombre,
                                Long idTokenFirma,
                                String observacion) {
        String actor = (actorNombre == null || actorNombre.isBlank())
                ? (actorId != null ? String.valueOf(actorId) : "SISTEMA")
                : actorNombre;

        actaHistorialRepository.save(ActaHistorial.builder()
                .idActa(idActa)
                .tipoEvento(tipoEvento)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .actorId(actorId)
                .actorNombre(actor)
                .idTokenFirma(idTokenFirma)
                .fechaCambio(LocalDateTime.now())
                .observacion(observacion)
                .build());
    }
}
