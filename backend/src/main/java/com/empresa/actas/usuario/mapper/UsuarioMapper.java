package com.empresa.actas.usuario.mapper;

import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "idUsuario", target = "id")
    @Mapping(source = "nombreUsuario", target = "username")
    @Mapping(source = "rol.nombre", target = "rol")
    UsuarioResponse toResponse(Usuario usuario);
}
