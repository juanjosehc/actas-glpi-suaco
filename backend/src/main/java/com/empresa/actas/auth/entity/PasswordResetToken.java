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
 * Mismo patron que {@code FirmaToken}: UUID de un solo uso con expiracion.
 * No se guarda el hash del token: se guarda el UUID en claro (comparable con
 * {@code findByToken}), igual que el token de firma publica del acta.
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

    @Column(name = "token", nullable = false, unique = true, length = 36)
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