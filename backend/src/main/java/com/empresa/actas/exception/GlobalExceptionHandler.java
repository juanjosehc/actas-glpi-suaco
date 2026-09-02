package com.empresa.actas.exception;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Traduce excepciones a respuestas JSON seguras (SEC-006): el cliente recibe
 * mensajes genericos, sin datos internos (correos, rutas, detalle GLPI, stack
 * traces). El detalle completo del error queda en los logs del servidor para
 * trazabilidad.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AuditoriaService auditoriaService;

    private static final Pattern PATRON_CORREO =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /** Quita correos del mensaje antes de exponerlo al cliente. */
    static String redactar(String mensaje) {
        if (mensaje == null) {
            return "Solicitud invalida";
        }
        return PATRON_CORREO.matcher(mensaje).replaceAll("[correo oculto]");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        // No distinguir "el usuario no existe" de "credencial invalida": evita
        // enumeracion de usuarios. El detalle queda en el log.
        log.warn("Intento de autenticacion con usuario inexistente: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Credenciales invalidas"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Credenciales invalidas"));
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ErrorResponse> handleAccountStatus(AccountStatusException ex) {
        // Spring Security usa mensajes internos en ingles (ej. "User account is locked").
        // Traducirlos a mensajes claros para el usuario final.
        String mensaje;
        if (ex instanceof LockedException) {
            mensaje = "Su cuenta se encuentra bloqueada. Por favor contacte al administrador.";
        } else if (ex instanceof DisabledException) {
            mensaje = "Su cuenta no se encuentra habilitada. Por favor contacte al administrador.";
        } else {
            mensaje = "El estado de su cuenta no permite iniciar sesion. Por favor contacte al administrador.";
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(mensaje));
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
        // Errores de validacion de bean (mensajes @Pattern/@Size/etc): contrato
        // de negocio, no exponen datos internos.
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
        // Validador de negocio (OTP, firma, token de firma, entidades). El mensaje
        // se conserva para el usuario, pero sin datos sensibles embebidos (correos).
        log.warn("Solicitud rechazada: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(redactar(ex.getMessage())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        // Recursos sin handler (ej. /uploads/** retirado del servido estatico) -> 404.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Recurso no encontrado"));
    }

    @ExceptionHandler(GlpiException.class)
    public ResponseEntity<ErrorResponse> handleGlpi(GlpiException ex) {
        // El detalle (status HTTP de GLPI, URLs, tokens) solo a los logs. Al
        // cliente un mensaje generico; el frontend distingue por el codigo 502.
        log.error("Error de integracion con GLPI", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(
                        "No se pudo conectar con el sistema de inventario. Intente nuevamente mas tarde."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            HttpServletRequest request, Exception ex) {
        // Nunca exponer ex.getMessage() al cliente: puede contener rutas, SQL,
        // nombres de clase o detalles del servidor. Trazabilidad en el log.
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Error interno del servidor"));
    }
}