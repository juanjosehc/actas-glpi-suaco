package com.empresa.actas.acta.mapper;

import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.entity.Acta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActaMapper {

    @Mapping(source = "idActa", target = "id")
    @Mapping(source = "tipoActa", target = "tipoActa")
    @Mapping(source = "estado", target = "estado")
    @Mapping(source = "fechaEnvio", target = "fechaEnvio")
    @Mapping(source = "fechaFirma", target = "fechaFirma")
    @Mapping(source = "fechaAprobacion", target = "fechaAprobacion")
    @Mapping(source = "observacionRechazo", target = "observacionRechazo")
    @Mapping(source = "fechaRechazo", target = "fechaRechazo")
    @Mapping(source = "rutaPdf", target = "rutaPdf")
    @Mapping(source = "rutaZip", target = "rutaZip")
    ActaResponse toResponse(Acta acta);
}

