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

@Entity
@Table(name = "firma_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long idToken;

    @Column(name = "id_acta", nullable = false)
    private Long idActa;

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

    /** Vencimiento del enlace (configurable, default 72h). Null = nunca expira (legacy). */
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;
}
