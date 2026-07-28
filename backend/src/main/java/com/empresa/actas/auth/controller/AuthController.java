package com.empresa.actas.auth.controller;

import com.empresa.actas.auth.dto.LoginRequest;
import com.empresa.actas.auth.dto.LoginResponse;
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
}
