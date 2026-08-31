package com.empresa.actas.acta.mapper;

import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.entity.Acta;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-27T14:59:30-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Oracle Corporation)"
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
        actaResponse.setContenidoHtml( acta.getContenidoHtml() );
        actaResponse.setFechaEnvio( acta.getFechaEnvio() );
        actaResponse.setFechaFirma( acta.getFechaFirma() );
        actaResponse.setFechaAprobacion( acta.getFechaAprobacion() );
        actaResponse.setObservacionRechazo( acta.getObservacionRechazo() );
        actaResponse.setFechaRechazo( acta.getFechaRechazo() );
        actaResponse.setRutaPdf( acta.getRutaPdf() );
        actaResponse.setCedulaUsuario( acta.getCedulaUsuario() );
        actaResponse.setNombreUsuario( acta.getNombreUsuario() );
        actaResponse.setCorreoUsuario( acta.getCorreoUsuario() );
        actaResponse.setSerialEquipo( acta.getSerialEquipo() );
        actaResponse.setPlacaEquipo( acta.getPlacaEquipo() );
        actaResponse.setDescripcionEquipo( acta.getDescripcionEquipo() );
        actaResponse.setFechaCreacion( acta.getFechaCreacion() );
        actaResponse.setTicketGlpi( acta.getTicketGlpi() );

        return actaResponse;
    }
}
