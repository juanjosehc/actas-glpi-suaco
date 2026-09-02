package com.empresa.actas.auth.controller;

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
 * SEC-011: cierre de sesion efectivo en servidor. Revoca el JWT actual por su
 * jti (denylist). La ruta queda fuera de /auth/** y /firma/** a proposito:
 * el JwtAuthenticationFilter no la salta, la autentica, y SecurityConfig la
 * trata como protegida (anyRequest().authenticated()), asi que el Bearer
 * llega ya validado.
 */
@RestController
@RequestMapping("/sesiones")
@RequiredArgsConstructor
@Tag(name = "Sesiones", description = "Gestion de sesiones JWT (SEC-011)")
public class SesionController {

    private final JwtService jwtService;
    private final JwtRevocadoRepository revocadoRepository;

    @PostMapping("/revocar")
    @Operation(summary = "Revoca el JWT de la peticion", description = "Logout efectivo: el token deja de validarse en el servidor aunque no haya expirado.")
    @Transactional
    public ResponseEntity<ErrorResponse> revocar(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String jti = jwtService.extraerJti(jwt);
                if (jti != null) {
                    Date expRaw = jwtService.extraerExpiracion(jwt);
                    LocalDateTime exp = expRaw != null
                            ? expRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : LocalDateTime.now().plusHours(1);
                    if (!revocadoRepository.existsByJti(jti)) {
                        revocadoRepository.save(JwtRevocado.builder()
                                .jti(jti)
                                .usuario(jwtService.extraerUsername(jwt))
                                .fechaRevocacion(LocalDateTime.now())
                                .fechaExpiracionToken(exp)
                                .build());
                    }
                    // Podado oportunista: los registros cuyo token original ya
                    // expiro no aportan nada (el JWT caduco por si solo).
                    revocadoRepository.deleteByFechaExpiracionTokenBefore(LocalDateTime.now());
                }
            } catch (Exception e) {
                // JWT invalido o ilegible: no hay sesion que revocar.
            }
        }
        return ResponseEntity.ok(ErrorResponse.ok("Sesion cerrada"));
    }
}