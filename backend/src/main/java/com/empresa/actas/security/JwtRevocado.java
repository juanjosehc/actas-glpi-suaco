package com.empresa.actas.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tabla de denylist de JWT revocados (SEC-011). Se guarda el {@code jti}
 * (identificador unico del token) para que el logout sea efectivo en servidor:
 * aunque el JWT no haya expirado, una vez aqui deja de validarse.
 * La expiracion del token original se conserva para poder limpiar filas viejas.
 */
@Entity
@Table(name = "jwt_revocado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtRevocado {

    @Id
    @Column(name = "jti", length = 64, nullable = false)
    private String jti;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Column(name = "fecha_revocacion", nullable = false)
    private LocalDateTime fechaRevocacion;

    @Column(name = "fecha_expiracion_token", nullable = false)
    private LocalDateTime fechaExpiracionToken;
}