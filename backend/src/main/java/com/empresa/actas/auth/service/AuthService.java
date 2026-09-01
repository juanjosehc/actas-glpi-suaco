package com.empresa.actas.auth.service;

import com.empresa.actas.auth.dto.LoginRequest;
import com.empresa.actas.auth.dto.LoginResponse;
import com.empresa.actas.auth.dto.RegisterUserRequest;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AuditoriaService auditoriaService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()));
        } catch (RuntimeException e) {
            // Usuario intentado (puede no existir) va como nombre; id null.
            auditoriaService.registrar(TipoEventoAuditoria.LOGIN_FALLIDO, null,
                    request.username(), "AUTENTICACION", null, "/auth/login",
                    "Intento de autenticacion fallido");
            throw e;
        }

        UserSecurity userSecurity = (UserSecurity) authentication.getPrincipal();
        String token = jwtService.generarToken(userSecurity);

        auditoriaService.registrar(TipoEventoAuditoria.LOGIN_EXITOSO,
                userSecurity.getUsuario().getIdUsuario(), userSecurity.getUsername(),
                "AUTENTICACION", null, "/auth/login", "Inicio de sesion exitoso");

        return new LoginResponse(
                token,
                userSecurity.getUsername(),
                userSecurity.getUsuario().getRol().getNombre());
    }

    /**
     * Cierre de sesion. Requiere token valido (ruta autenticada); registra
     * el usuario que abandona la sesion en la CAPA 2.
     */
    public void logout() {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
        auditoriaService.registrar(TipoEventoAuditoria.LOGOUT,
                userSecurity.getUsuario().getIdUsuario(), userSecurity.getUsername(),
                "AUTENTICACION", null, "/auth/logout", "Cierre de sesion");
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

        // REGISTRO PUBLICO => SIEMPRE TECNICO. El rol se decide en el servidor,
        // nunca desde el request (el DTO exponia rol libre y permitia crear
        // cuentas ADMINISTRADOR via POST /auth/register).
        // La creacion con rol elegido queda en POST /usuarios (solo ADMINISTRADOR).
        Rol rol = rolRepository.findByNombre("TECNICO")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol TECNICO no configurado en la base de datos"));

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
