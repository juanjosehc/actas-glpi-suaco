package com.empresa.actas.auth.service;

import com.empresa.actas.auth.dto.LoginRequest;
import com.empresa.actas.auth.dto.LoginResponse;
import com.empresa.actas.auth.dto.RegisterUserRequest;
import com.empresa.actas.security.JwtService;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.rol.repository.RolRepository;
import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.repository.UsuarioRepository;
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
            throw new IllegalArgumentException(
                    "El nombre de usuario ya existe: " + request.username());
        }

        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException(
                    "El correo ya esta registrado: " + request.correo());
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

        return usuarioRepository.save(usuario);
    }
}
