package com.empresa.actas.auth.controller;

import com.empresa.actas.auth.dto.CambiarPasswordRequest;
import com.empresa.actas.auth.dto.ConfirmarRecuperacionRequest;
import com.empresa.actas.auth.dto.LoginRequest;
import com.empresa.actas.auth.dto.LoginResponse;
import com.empresa.actas.auth.dto.RecuperarPasswordRequest;
import com.empresa.actas.auth.dto.RegisterUserRequest;
import com.empresa.actas.auth.service.AuthService;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.usuario.entity.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Endpoints de login y registro de usuarios")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica un usuario y retorna un token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public ResponseEntity<ErrorResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ErrorResponse.ok("Login exitoso", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Registra el cierre de sesion del usuario autenticado")
    public ResponseEntity<ErrorResponse> logout() {
        authService.logout();
        return ResponseEntity.ok(ErrorResponse.ok("Sesion cerrada"));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario en el sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o usuario ya existe")
    })
    public ResponseEntity<ErrorResponse> register(
            @Valid @RequestBody RegisterUserRequest request) {
        Usuario usuario = authService.registrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ErrorResponse.ok("Usuario registrado exitosamente",
                        usuario.getNombreUsuario()));
    }

    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar contrasena", description = "Cambia la contrasena del usuario autenticado (any rol). Valida la contrasena actual y aplica SEC-016. Registra CAMBIO_CONTRASENA en auditoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrasena cambiada"),
            @ApiResponse(responseCode = "400", description = "Contrasena actual incorrecta o nueva invalida"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ErrorResponse> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(request);
        return ResponseEntity.ok(ErrorResponse.ok("Contrasena cambiada correctamente"));
    }

    @PostMapping("/recuperar")
    @Operation(summary = "Solicitar recuperacion de contrasena",
            description = "Envia al correo un enlace de un solo uso con un token expiracion (por defecto 30 min). Respuesta generica en todos los casos para no revelar si el correo existe (anti-enumeracion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada"),
            @ApiResponse(responseCode = "400", description = "Correo invalido")
    })
    public ResponseEntity<ErrorResponse> recuperar(
            @Valid @RequestBody RecuperarPasswordRequest request) {
        authService.solicitarRecuperacion(request);
        return ResponseEntity.ok(ErrorResponse.ok(
                "Si el correo esta registrado, recibiras un enlace para restablecer tu contrasena"));
    }

    @PostMapping("/recuperar/confirmar")
    @Operation(summary = "Confirmar recuperacion con el token del correo",
            description = "Valida el token de un solo uso (no vencido, no utilizado) y establece la nueva contrasena SEC-016. Tokens invalidos se rechazan y auditan como RECUPERACION_TOKEN_INVALIDO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrasena restablecida"),
            @ApiResponse(responseCode = "400", description = "Token invalido, vencido o ya utilizado")
    })
    public ResponseEntity<ErrorResponse> confirmarRecuperacion(
            @Valid @RequestBody ConfirmarRecuperacionRequest request) {
        authService.confirmarRecuperacion(request);
        return ResponseEntity.ok(ErrorResponse.ok(
                "Contrasena restablecida. Ya puedes iniciar sesion con la nueva contrasena"));
    }
}
