package com.empresa.actas.auth.controller;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.security.JwtRevocado;
import com.empresa.actas.security.JwtRevocadoRepository;
import com.empresa.actas.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * SEC-011/SEC-118: LOGOUT unico y efectivo en servidor. Revoca el JWT actual por su
 * jti (denylist persistente) y registra LOGOUT en la CAPA 2. Ruta fuera de /auth/**
 * y /firma/** a proposito: el JwtAuthenticationFilter la autentica y SecurityConfig
 * la trata como protegida (anyRequest().authenticated()), asi que el Bearer llega
 * ya validado y SIEMPRE hay una sesion que revocar.
 *
 * Se elimino el dual /auth/logout (que solo auditaba): aqui se une revocacion +
 * auditoria, y un fallo al escribir la denylist NO se traga — responde 500 para
 * que el cliente sepa que el token sigue vivo.
 */
@RestController
@RequestMapping("/sesiones")
@RequiredArgsConstructor
@Tag(name = "Sesiones", description = "Gestion de sesiones JWT (SEC-011)")
public class SesionController {

    private final JwtService jwtService;
    private final JwtRevocadoRepository revocadoRepository;
    private final AuditoriaService auditoriaService;

    @PostMapping("/revocar")
    @Operation(summary = "Revoca el JWT de la peticion", description = "Logout efectivo: el token deja de validarse en el servidor aunque no haya expirado, y se audita el cierre de sesion.")
    @Transactional
    public ResponseEntity<ErrorResponse> revocar(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String jti = jwtService.extraerJti(jwt);
            if (jti != null) {
                Date expRaw = jwtService.extraerExpiracion(jwt);
                LocalDateTime exp = expRaw != null
                        ? expRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : LocalDateTime.now().plusHours(1);
                // SEC-118: si la denylist no se puede escribir, la excepcion
                // propaga -> 500. El cliente sabe que la revocacion fallo.
                if (!revocadoRepository.existsByJti(jti)) {
                    revocadoRepository.save(JwtRevocado.builder()
                            .jti(jti)
                            .usuario(jwtService.extraerUsername(jwt))
                            .fechaRevocacion(LocalDateTime.now())
                            .fechaExpiracionToken(exp)
                            .build());
                }
                // Podado oportunista: registros cuyo token ya vencio no aportan.
                revocadoRepository.deleteByFechaExpiracionTokenBefore(LocalDateTime.now());
            }
        }
        // Auditoria del cierre (CAPA 2): el actor es el principal autenticado por
        // el filtro (esta ruta es protegida), asi que el overload lee el contexto.
        auditoriaService.registrar(TipoEventoAuditoria.LOGOUT,
                "AUTENTICACION", null, "/sesiones/revocar",
                "Cierre de sesion (JWT revocado por jti)");
        return ResponseEntity.ok(ErrorResponse.ok("Sesion cerrada"));
    }
}