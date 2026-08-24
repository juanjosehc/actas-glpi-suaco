package com.empresa.actas.exception;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditoriaService auditoriaService;

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Credenciales invalidas"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            HttpServletRequest request, AccessDeniedException ex) {
        // 403 -> auditoria de sistema: quien intento acceder (o anonimo) y a que recurso.
        auditoriaService.registrar(TipoEventoAuditoria.ACCESO_DENEGADO,
                "RECURSO",
                request.getRequestURI(),
                request.getRequestURI(),
                ex.getMessage() != null ? ex.getMessage() : "Acceso denegado");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("No tiene permisos para realizar esta accion"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Valor invalido",
                        (a, b) -> b));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Errores de validacion", errores));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        // Recursos sin handler (ej. /uploads/** retirado del servido estatico) -> 404.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Recurso no encontrado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Error interno: " + ex.getMessage()));
    }
}
