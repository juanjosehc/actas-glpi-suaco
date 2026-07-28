package com.empresa.actas.usuario.service;

import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.rol.repository.RolRepository;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.usuario.dto.ActualizarUsuarioRequest;
import com.empresa.actas.usuario.dto.CrearUsuarioRequest;
import com.empresa.actas.usuario.dto.UsuarioResponse;
import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.mapper.UsuarioMapper;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(usuarioMapper::toResponse);
    }

    public UsuarioResponse obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse obtenerUsuarioActual() {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
        return usuarioMapper.toResponse(userSecurity.getUsuario());
    }

    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByNombreUsuario(request.username())) {
            throw new IllegalArgumentException(
                    "El nombre de usuario ya existe: " + request.username());
        }

        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "El correo ya esta registrado: " + request.correo());
        }

        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new IllegalArgumentException(
                    "La cedula ya esta registrada: " + request.cedula());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + request.rol()));

        Usuario usuario = Usuario.builder()
                .cedula(request.cedula())
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .nombreUsuario(request.username())
                .correo(request.correo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .cargo(request.cargo())
                .empresa(request.empresa())
                .lugarTrabajo(request.lugarTrabajo())
                .bloqueado(false)
                .rol(rol)
                .build();

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        if (!usuario.getCorreo().equals(request.correo())
                && usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "El correo ya esta registrado: " + request.correo());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + request.rol()));

        usuario.setNombres(request.nombres());
        usuario.setApellidos(request.apellidos());
        usuario.setCorreo(request.correo());
        usuario.setCargo(request.cargo());
        usuario.setEmpresa(request.empresa());
        usuario.setLugarTrabajo(request.lugarTrabajo());
        usuario.setRol(rol);

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse bloquearUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        usuario.setBloqueado(true);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse desbloquearUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con id: " + id));

        usuario.setBloqueado(false);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }
}
