package com.empresa.actas.firma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OTP del portal publico de firma (segunda capa de seguridad).
 *
 * Solo se persiste {@code codigo_hash} (BCrypt), jamas el codigo en claro:
 * quien tenga acceso a la BD no puede leer los OTP emitidos. El codigo viaja
 * unicamente por el correo al destinatario y por el request de validacion.
 */
@Entity
@Table(name = "firma_otp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmaOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_otp")
    private Long idOtp;

    @Column(name = "id_token_firma", nullable = false)
    private Long idTokenFirma;

    @Column(name = "codigo_hash", nullable = false, length = 60)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    /** UUID emitido al validar el codigo; prueba de sesion ante {@code verificarSesion}. */
    @Column(name = "sesion", unique = true)
    private String sesion;

    @Column(name = "correo_destino", nullable = false, length = 255)
    private String correoDestino;
}