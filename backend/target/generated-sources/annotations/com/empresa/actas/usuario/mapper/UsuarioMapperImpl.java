package com.empresa.actas.usuario.mapper;

import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.entity.Usuario;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T11:38:29-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Oracle Corporation)"
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
        usuarioResponse.setCedula( usuario.getCedula() );
        usuarioResponse.setNombres( usuario.getNombres() );
        usuarioResponse.setApellidos( usuario.getApellidos() );
        usuarioResponse.setCorreo( usuario.getCorreo() );
        usuarioResponse.setCargo( usuario.getCargo() );
        usuarioResponse.setEmpresa( usuario.getEmpresa() );
        usuarioResponse.setLugarTrabajo( usuario.getLugarTrabajo() );
        usuarioResponse.setBloqueado( usuario.getBloqueado() );

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
