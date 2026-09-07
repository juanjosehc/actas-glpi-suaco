package com.empresa.actas.auth.entity;

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
 * Token de un solo uso para recuperar una contrasena olvidada.
 *
 * SEC-120: a diferencia de {@code FirmaToken} (que una vez usado no surge
 * necesidad de re-mostrarlo al personal), este SI se persiste como digest
 * SHA-256 hex del UUID original ({@link TokenDigest}): quien tenga un dump de
 * la BD no obtiene la capability. El UUID en claro viaja solo en el correo y
 * en la URL (fragmento, ver recuperar.html). La busqueda usa el digest del
 * valor recibido ({@code findByTokenHash}).
 */
@Entity
@Table(name = "password_reset_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long idToken;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    /** SEC-120: digest SHA-256 hex (64 chars) del UUID original, no la capability en claro. */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "utilizado", nullable = false)
    @Builder.Default
    private Boolean utilizado = false;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_utilizacion")
    private LocalDateTime fechaUtilizacion;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;
}