package com.empresa.actas.auth.service;

import com.empresa.actas.auth.dto.CambiarPasswordRequest;
import com.empresa.actas.auth.dto.ConfirmarRecuperacionRequest;
import com.empresa.actas.auth.dto.LoginRequest;
import com.empresa.actas.auth.dto.LoginResponse;
import com.empresa.actas.auth.dto.RecuperarPasswordRequest;
import com.empresa.actas.auth.dto.RegisterUserRequest;
import com.empresa.actas.auth.entity.PasswordResetToken;
import com.empresa.actas.auth.repository.PasswordResetTokenRepository;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.mail.service.MailService;
import com.empresa.actas.security.JwtService;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.rol.repository.RolRepository;
import com.empresa.actas.usuario.entity.Usuario;
import com.empresa.actas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    /** URL base del portal (login.html?…): base del enlace de recuperacion. */
    @Value("${app.firma-url-base:}")
    private String firmaUrlBase;

    /** Vigencia del enlace de recuperacion en minutos (default 30). */
    @Value("${app.recuperacion-token-expira-minutos:30}")
    private long recuperacionExpiraMinutos;

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
                userSecurity.getUsuario().getRol().getNombre(),
                Boolean.TRUE.equals(userSecurity.getUsuario().getCambiarPasswordObligatorio()));
    }

    /**
     * Cambio de contrasena por autoservicio (usuario autenticado, cualquier rol).
     * Valida la contrasena actual y aplica la politica SEC-016 a la nueva.
     * Registra CAMBIO_CONTRASENA en auditoria.
     */
    public void cambiarPassword(CambiarPasswordRequest request) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
        Usuario usuario = userSecurity.getUsuario();

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasena actual es incorrecta.");
        }
        if (request.passwordActual().equals(request.nuevaPassword())) {
            throw new IllegalArgumentException("La nueva contrasena no puede ser igual a la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuario.setCambiarPasswordObligatorio(false);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(TipoEventoAuditoria.CAMBIO_CONTRASENA,
                usuario.getIdUsuario(), usuario.getNombreUsuario(),
                "USUARIO", String.valueOf(usuario.getIdUsuario()), "/auth/cambiar-password",
                "Cambio de contrasena propio");
    }

    /**
     * Solicitud de recuperacion por correo. Respuesta GENERICA siempre
     * (anti-enumeracion): no revela si el correo existe. Invalida tokens
     * anteriores del usuario (una sola recuperacion activa). Si SMTP no esta
     * configurado, el enlace no se envia y solo queda el registro de auditoria
     * y el log (el bypass por SQL sigue documentado para ese caso).
     */
    @Transactional
    public void solicitarRecuperacion(RecuperarPasswordRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Optional<Usuario> opt = usuarioRepository.findByCorreo(correo);

        if (opt.isEmpty()) {
            auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_SOLICITADA,
                    null, correo, "USUARIO", correo, "/auth/recuperar",
                    "Solicitud de recuperacion para correo no registrado");
            return;
        }

        Usuario usuario = opt.get();
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            // Cuenta bloqueada: no se envia enlace (desbloquear es decision del
            // administrador), pero se audita la solicitud.
            auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_SOLICITADA,
                    usuario.getIdUsuario(), usuario.getNombreUsuario(),
                    "USUARIO", correo, "/auth/recuperar",
                    "Solicitud de recuperacion para cuenta bloqueada (sin envio)");
            return;
        }

        passwordResetTokenRepository.deleteByIdUsuario(usuario.getIdUsuario());
        String token = UUID.randomUUID().toString();
        LocalDateTime expira = LocalDateTime.now().plusMinutes(recuperacionExpiraMinutos);
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .idUsuario(usuario.getIdUsuario())
                .token(token)
                .fechaExpiracion(expira)
                .build());

        String url = firmaUrlBase + "/recuperar.html?token=" + token;
        boolean enviado = mailService.enviarCorreoRecuperacion(
                correo, usuario.getNombres(), url, recuperacionExpiraMinutos);
        if (!enviado) {
            log.warn("Recuperacion solicitada para '{}' pero el correo no se envio (SMTP no configurado o error)", correo);
        }

        auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_SOLICITADA,
                usuario.getIdUsuario(), usuario.getNombreUsuario(),
                "USUARIO", correo, "/auth/recuperar",
                "Solicitud de recuperacion " + (enviado ? "con enlace enviado" : "sin envio (SMTP no disponible)"));
    }

    /**
     * Confirma la recuperacion con el token del correo (un solo uso, con
     * expiracion) y establece la nueva contrasena SEC-016. Tokens invalidos,
     * expirados o ya usados se rechazan y se auditan.
     */
    public void confirmarRecuperacion(ConfirmarRecuperacionRequest request) {
        PasswordResetToken row = passwordResetTokenRepository
                .findByToken(request.token().trim())
                .orElseThrow(() -> {
                    auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_TOKEN_INVALIDO,
                            null, null, "USUARIO", request.token(), "/auth/recuperar/confirmar",
                            "Token de recuperacion inexistente");
                    return new IllegalArgumentException(
                            "El enlace de recuperacion no es valido o ya fue utilizado.");
                });

        boolean utilizado = Boolean.TRUE.equals(row.getUtilizado());
        boolean expirado = row.getFechaExpiracion() != null
                && row.getFechaExpiracion().isBefore(LocalDateTime.now());
        if (utilizado || expirado) {
            auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_TOKEN_INVALIDO,
                    row.getIdUsuario(), null, "USUARIO", row.getToken(), "/auth/recuperar/confirmar",
                    "Token de recuperacion " + (utilizado ? "ya utilizado" : "expirado"));
            throw new IllegalArgumentException(
                    "El enlace de recuperacion no es valido o ya fue utilizado.");
        }

        Usuario usuario = usuarioRepository.findById(row.getIdUsuario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario asociado al enlace ya no existe."));

        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuario.setCambiarPasswordObligatorio(false);
        usuarioRepository.save(usuario);

        row.setUtilizado(true);
        row.setFechaUtilizacion(LocalDateTime.now());
        passwordResetTokenRepository.save(row);

        auditoriaService.registrar(TipoEventoAuditoria.RECUPERACION_COMPLETADA,
                usuario.getIdUsuario(), usuario.getNombreUsuario(),
                "USUARIO", String.valueOf(usuario.getIdUsuario()), "/auth/recuperar/confirmar",
                "Recuperacion de contrasena completada");
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
