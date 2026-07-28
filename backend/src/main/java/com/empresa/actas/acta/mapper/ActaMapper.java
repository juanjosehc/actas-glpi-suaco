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
    ActaResponse toResponse(Acta acta);
}

