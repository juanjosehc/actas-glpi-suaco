package com.actasglpi.auth.service;

import com.actasglpi.auth.dto.LoginRequest;
import com.actasglpi.auth.dto.LoginResponse;
import com.actasglpi.auth.dto.RegisterUserRequest;
import com.actasglpi.auth.security.JwtService;
import com.actasglpi.auth.security.UserSecurity;
import com.actasglpi.rol.entity.Rol;
import com.actasglpi.rol.repository.RolRepository;
import com.actasglpi.usuario.entity.Usuario;
import com.actasglpi.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        UserSecurity userSecurity = (UserSecurity) authentication.getPrincipal();
        String token = jwtService.generarToken(userSecurity);

        return new LoginResponse(
                token,
                userSecurity.getUsername(),
                userSecurity.getUsuario().getRol().getNombre());
    }

    public Usuario registrarUsuario(RegisterUserRequest request) {
        if (usuarioRepository.existsByNombreUsuario(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe: " + request.username());
        }

        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya esta registrado: " + request.correo());
        }

        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + request.rol()));

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

        return usuarioRepository.save(usuario);
    }
}
