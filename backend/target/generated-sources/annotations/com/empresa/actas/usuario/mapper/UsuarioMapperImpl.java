package com.empresa.actas.usuario.mapper;

import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.entity.Usuario;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-04T08:36:32-0500",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public UsuarioResponse toResponse(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioResponse usuarioResponse = new UsuarioResponse();

        usuarioResponse.setId( usuario.getIdUsuario() );
        usuarioResponse.setUsername( usuario.getNombreUsuario() );
        usuarioResponse.setRol( usuarioRolNombre( usuario ) );
        usuarioResponse.setApellidos( usuario.getApellidos() );
        usuarioResponse.setBloqueado( usuario.getBloqueado() );
        usuarioResponse.setCargo( usuario.getCargo() );
        usuarioResponse.setCedula( usuario.getCedula() );
        usuarioResponse.setCorreo( usuario.getCorreo() );
        usuarioResponse.setEmpresa( usuario.getEmpresa() );
        usuarioResponse.setLugarTrabajo( usuario.getLugarTrabajo() );
        usuarioResponse.setNombres( usuario.getNombres() );

        return usuarioResponse;
    }

    private String usuarioRolNombre(Usuario usuario) {
        Rol rol = usuario.getRol();
        if ( rol == null ) {
            return null;
        }
        return rol.getNombre();
    }
}
