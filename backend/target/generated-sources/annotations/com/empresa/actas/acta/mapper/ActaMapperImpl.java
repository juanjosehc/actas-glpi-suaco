package com.empresa.actas.acta.mapper;

import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.entity.Acta;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-04T11:47:43-0500",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class ActaMapperImpl implements ActaMapper {

    @Override
    public ActaResponse toResponse(Acta acta) {
        if ( acta == null ) {
            return null;
        }

        ActaResponse actaResponse = new ActaResponse();

        actaResponse.setId( acta.getIdActa() );
        if ( acta.getTipoActa() != null ) {
            actaResponse.setTipoActa( acta.getTipoActa().name() );
        }
        if ( acta.getEstado() != null ) {
            actaResponse.setEstado( acta.getEstado().name() );
        }
        actaResponse.setFechaEnvio( acta.getFechaEnvio() );
        actaResponse.setFechaFirma( acta.getFechaFirma() );
        actaResponse.setFechaAprobacion( acta.getFechaAprobacion() );
        actaResponse.setObservacionRechazo( acta.getObservacionRechazo() );
        actaResponse.setFechaRechazo( acta.getFechaRechazo() );
        actaResponse.setRutaPdf( acta.getRutaPdf() );
        actaResponse.setRutaZip( acta.getRutaZip() );
        actaResponse.setCedulaUsuario( acta.getCedulaUsuario() );
        actaResponse.setCorreoUsuario( acta.getCorreoUsuario() );
        actaResponse.setDescripcionEquipo( acta.getDescripcionEquipo() );
        actaResponse.setFechaCreacion( acta.getFechaCreacion() );
        actaResponse.setNombreUsuario( acta.getNombreUsuario() );
        actaResponse.setPlacaEquipo( acta.getPlacaEquipo() );
        actaResponse.setRutaPdfChecklist( acta.getRutaPdfChecklist() );
        actaResponse.setSerialEquipo( acta.getSerialEquipo() );
        actaResponse.setTicketGlpi( acta.getTicketGlpi() );

        return actaResponse;
    }
}
